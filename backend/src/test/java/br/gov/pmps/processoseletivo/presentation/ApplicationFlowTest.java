package br.gov.pmps.processoseletivo.presentation;

import static br.gov.pmps.processoseletivo.support.ProcessFixtures.PDF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.pmps.processoseletivo.PostgresIntegrationTest;
import br.gov.pmps.processoseletivo.application.service.EmailOutboxService;
import br.gov.pmps.processoseletivo.application.usecase.application.DiscardExpiredDraftsUseCase;
import br.gov.pmps.processoseletivo.application.usecase.process.AdvanceProcessStatusesUseCase;
import br.gov.pmps.processoseletivo.support.AdminTestAccounts;
import br.gov.pmps.processoseletivo.support.AuthTestClient;
import br.gov.pmps.processoseletivo.support.AuthTestClient.RegisteredCandidate;
import br.gov.pmps.processoseletivo.support.ProcessFixtures;
import br.gov.pmps.processoseletivo.support.ProcessFixtures.CreatedProcess;
import br.gov.pmps.processoseletivo.support.ProcessFixtures.Requirement;
import br.gov.pmps.processoseletivo.support.RecordingEmailGateway;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
class ApplicationFlowTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private RecordingEmailGateway emailGateway;

    @Autowired
    private EmailOutboxService emailOutbox;

    @Autowired
    private AdvanceProcessStatusesUseCase advanceProcessStatuses;

    @Autowired
    private DiscardExpiredDraftsUseCase discardExpiredDrafts;

    private ProcessFixtures processes;
    private AuthTestClient auth;

    @BeforeEach
    void setUp() throws Exception {
        Cookie admin = new AdminTestAccounts(jdbcTemplate, passwordEncoder, mockMvc).sessionWith(
                "PROCESSO_VISUALIZAR", "PROCESSO_CRIAR", "PROCESSO_EDITAR", "PROCESSO_PUBLICAR", "PROCESSO_ENCERRAR");
        processes = new ProcessFixtures(mockMvc, jsonMapper, admin);
        auth = new AuthTestClient(mockMvc);
    }

    @Nested
    class Confirmation {

        @Test
        void fullFlowGeneratesNumberSnapshotReceiptAndEmail() throws Exception {
            CreatedProcess process = processes.openProcess(false, 1,
                    new Requirement("Documento de identidade", true), new Requirement("Certificado", false));
            RegisteredCandidate candidate = auth.registerCandidate();
            Cookie session = auth.login(candidate.cpf(), candidate.password());

            UUID applicationId = start(session, process.id(), process.positionIds().get(0));
            uploadDocument(session, applicationId, process.requirementIds().get(0), pdf()).andExpect(status().isCreated());
            String number = confirm(session, applicationId).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(number).contains(process.number() + "/2026-00001");
            mockMvc.perform(get("/api/candidate/applications/{id}", applicationId).cookie(session))
                    .andExpect(jsonPath("$.status").value("RECEBIDA"))
                    .andExpect(jsonPath("$.applicationNumber").value(process.number() + "/2026-00001"))
                    // Documentos exigidos vêm em ordem alfabética: "Certificado" antes de "Documento de identidade".
                    .andExpect(jsonPath("$.requirements[1].documents[0].originalName").value("documento.pdf"));
            mockMvc.perform(get("/api/candidate/applications").cookie(session))
                    .andExpect(jsonPath("$[0].positionName").value("Cargo 1"));

            byte[] receipt = mockMvc.perform(get("/api/candidate/applications/{id}/receipt", applicationId).cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                    .andReturn().getResponse().getContentAsByteArray();
            assertThat(new String(receipt, 0, 5)).isEqualTo("%PDF-");

            emailOutbox.dispatchDue();
            assertThat(emailGateway.awaitMessageTo(candidate.email()).body())
                    .contains(process.number() + "/2026-00001")
                    .doesNotContain(candidate.cpf());
        }

        @Test
        void publicVerificationShowsNoPersonalData() throws Exception {
            CreatedProcess process = processes.openProcess(false, 1);
            RegisteredCandidate candidate = auth.registerCandidate();
            Cookie session = auth.login(candidate.cpf(), candidate.password());
            UUID applicationId = start(session, process.id(), process.positionIds().get(0));
            confirm(session, applicationId).andExpect(status().isOk());
            String code = jdbcTemplate.queryForObject(
                    "select verification_code from application where id = ?", String.class, applicationId);

            String body = mockMvc.perform(get("/api/receipts/{code}", code.toLowerCase().replace("-", "")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.positionName").value("Cargo 1"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContain(candidate.cpf(), candidate.email(), "Maria");
            mockMvc.perform(get("/api/receipts/{code}", "AAAA-BBBB-CCCC")).andExpect(status().isNotFound());
        }

        @Test
        void requiresMandatoryDocuments() throws Exception {
            CreatedProcess process = processes.openProcess(false, 1, new Requirement("Diploma", true));
            Cookie session = newCandidateSession();
            UUID applicationId = start(session, process.id(), process.positionIds().get(0));

            confirm(session, applicationId)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasItem("Diploma")));
        }

        @Test
        void snapshotKeepsDataFromConfirmationTime() throws Exception {
            CreatedProcess process = processes.openProcess(false, 1);
            RegisteredCandidate candidate = auth.registerCandidate();
            Cookie session = auth.login(candidate.cpf(), candidate.password());
            UUID applicationId = start(session, process.id(), process.positionIds().get(0));
            confirm(session, applicationId).andExpect(status().isOk());

            mockMvc.perform(put("/api/candidate/me").with(csrf()).cookie(session)
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"fullName": "Nome Alterado Depois", "birthDate": "1990-03-15",
                                     "motherName": "Joana Teste Silva", "phone": "24999990000", "cep": "25850000",
                                     "street": "Rua Nova", "addressNumber": "1", "complement": null,
                                     "neighborhood": "Centro", "city": "Paraíba do Sul", "uf": "RJ",
                                     "hasDisability": false, "adaptations": []}
                                    """))
                    .andExpect(status().isOk());

            assertThat(jdbcTemplate.queryForObject(
                    "select full_name from application_snapshot where application_id = ?", String.class, applicationId))
                    .isEqualTo("Maria Teste Silva");
        }

        @Test
        void confirmedApplicationIsLocked() throws Exception {
            CreatedProcess process = processes.openProcess(false, 1, new Requirement("RG", true));
            Cookie session = newCandidateSession();
            UUID applicationId = start(session, process.id(), process.positionIds().get(0));
            UUID documentId = idFrom(uploadDocument(session, applicationId, process.requirementIds().get(0), pdf()));
            confirm(session, applicationId).andExpect(status().isOk());

            confirm(session, applicationId).andExpect(status().isConflict());
            uploadDocument(session, applicationId, process.requirementIds().get(0), pdf()).andExpect(status().isConflict());
            mockMvc.perform(delete("/api/candidate/applications/{id}/documents/{doc}", applicationId, documentId)
                    .with(csrf()).cookie(session)).andExpect(status().isConflict());
            mockMvc.perform(delete("/api/candidate/applications/{id}", applicationId).with(csrf()).cookie(session))
                    .andExpect(status().isConflict());
        }

        @Test
        void concurrentConfirmationsReceiveDistinctSequentialNumbers() throws Exception {
            CreatedProcess process = processes.openProcess(false, 1);
            int candidates = 5;
            List<Cookie> sessions = new ArrayList<>();
            List<UUID> applications = new ArrayList<>();
            for (int i = 0; i < candidates; i++) {
                Cookie session = newCandidateSession();
                sessions.add(session);
                applications.add(start(session, process.id(), process.positionIds().get(0)));
            }

            List<Integer> statuses = runConcurrently(candidates,
                    index -> confirm(sessions.get(index), applications.get(index)).andReturn().getResponse().getStatus());

            assertThat(statuses).containsOnly(200);
            List<Integer> sequences = jdbcTemplate.queryForList(
                    "select sequence_number from application where process_id = ? order by sequence_number",
                    Integer.class, process.id());
            assertThat(sequences).containsExactly(1, 2, 3, 4, 5);
        }
    }

    @Nested
    class SingleApplicationRule {

        @Test
        void onlyOneApplicationPerProcessByDefault() throws Exception {
            CreatedProcess process = processes.openProcess(false, 2);
            Cookie session = newCandidateSession();

            start(session, process.id(), process.positionIds().get(0));
            startRequest(session, process.id(), process.positionIds().get(1)).andExpect(status().isConflict());
        }

        @Test
        void concurrentStartsCreateOnlyOneApplication() throws Exception {
            CreatedProcess process = processes.openProcess(false, 1);
            Cookie session = newCandidateSession();

            List<Integer> statuses = runConcurrently(6,
                    index -> startRequest(session, process.id(), process.positionIds().get(0))
                            .andReturn().getResponse().getStatus());

            assertThat(statuses).filteredOn(code -> code == 201).hasSize(1);
            assertThat(statuses).filteredOn(code -> code != 201).containsOnly(409);
        }

        @Test
        void oneApplicationPerPositionWhenProcessAllowsMultiple() throws Exception {
            CreatedProcess process = processes.openProcess(true, 2);
            Cookie session = newCandidateSession();

            start(session, process.id(), process.positionIds().get(0));
            start(session, process.id(), process.positionIds().get(1));
            startRequest(session, process.id(), process.positionIds().get(0)).andExpect(status().isConflict());
        }

        @Test
        void positionMustBelongToProcess() throws Exception {
            CreatedProcess first = processes.openProcess(false, 1);
            CreatedProcess second = processes.openProcess(false, 1);
            Cookie session = newCandidateSession();

            startRequest(session, first.id(), second.positionIds().get(0)).andExpect(status().isConflict());
        }
    }

    @Nested
    class Ownership {

        @Test
        void anotherCandidateCannotAccessApplication() throws Exception {
            CreatedProcess process = processes.openProcess(false, 1, new Requirement("RG", true));
            Cookie owner = newCandidateSession();
            Cookie intruder = newCandidateSession();
            UUID applicationId = start(owner, process.id(), process.positionIds().get(0));
            UUID documentId = idFrom(uploadDocument(owner, applicationId, process.requirementIds().get(0), pdf()));

            mockMvc.perform(get("/api/candidate/applications/{id}", applicationId).cookie(intruder))
                    .andExpect(status().isNotFound());
            mockMvc.perform(get("/api/candidate/applications/{id}/documents/{doc}/file", applicationId, documentId)
                    .cookie(intruder)).andExpect(status().isNotFound());
            uploadDocument(intruder, applicationId, process.requirementIds().get(0), pdf())
                    .andExpect(status().isNotFound());
            confirm(intruder, applicationId).andExpect(status().isNotFound());
            mockMvc.perform(delete("/api/candidate/applications/{id}", applicationId).with(csrf()).cookie(intruder))
                    .andExpect(status().isNotFound());
        }

        @Test
        void administratorsCannotUseCandidateApplicationEndpoints() throws Exception {
            mockMvc.perform(get("/api/candidate/applications").with(user("admin").roles("ADMIN")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class Documents {

        @Test
        void validatesSizeTypeAndRequirement() throws Exception {
            CreatedProcess process = processes.openProcess(false, 1, new Requirement("RG", true));
            CreatedProcess other = processes.openProcess(false, 1, new Requirement("Outro", true));
            Cookie session = newCandidateSession();
            UUID applicationId = start(session, process.id(), process.positionIds().get(0));
            UUID requirementId = process.requirementIds().get(0);

            byte[] tooLarge = new byte[2 * 1024 * 1024 + 1];
            System.arraycopy(PDF, 0, tooLarge, 0, PDF.length);
            uploadDocument(session, applicationId, requirementId,
                    new MockMultipartFile("file", "grande.pdf", "application/pdf", tooLarge))
                    .andExpect(status().isBadRequest());
            uploadDocument(session, applicationId, requirementId,
                    new MockMultipartFile("file", "script.pdf", "application/pdf", "<html>".getBytes()))
                    .andExpect(status().isBadRequest());
            uploadDocument(session, applicationId, other.requirementIds().get(0), pdf())
                    .andExpect(status().isConflict());
            uploadDocument(session, applicationId, requirementId,
                    new MockMultipartFile("file", "foto.png", "image/png",
                            new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00}))
                    .andExpect(status().isCreated());
        }

        @Test
        void limitsFilesPerApplication() throws Exception {
            CreatedProcess process = processes.openProcess(false, 1, new Requirement("Certificados", false));
            Cookie session = newCandidateSession();
            UUID applicationId = start(session, process.id(), process.positionIds().get(0));

            for (int i = 0; i < 20; i++) {
                uploadDocument(session, applicationId, process.requirementIds().get(0), pdf())
                        .andExpect(status().isCreated());
            }
            uploadDocument(session, applicationId, process.requirementIds().get(0), pdf())
                    .andExpect(status().isConflict());
        }

        @Test
        void draftCanRemoveDocumentAndStoredFileIsDeleted() throws Exception {
            CreatedProcess process = processes.openProcess(false, 1, new Requirement("RG", true));
            Cookie session = newCandidateSession();
            UUID applicationId = start(session, process.id(), process.positionIds().get(0));
            UUID documentId = idFrom(uploadDocument(session, applicationId, process.requirementIds().get(0), pdf()));

            mockMvc.perform(delete("/api/candidate/applications/{id}/documents/{doc}", applicationId, documentId)
                    .with(csrf()).cookie(session)).andExpect(status().isNoContent());

            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from application_document where id = ?", Integer.class, documentId)).isZero();
        }
    }

    @Nested
    class Period {

        @Test
        void closedProcessRejectsConfirmationAndDraftsAreDiscarded() throws Exception {
            CreatedProcess process = processes.openProcess(false, 1, new Requirement("RG", false));
            Cookie session = newCandidateSession();
            UUID applicationId = start(session, process.id(), process.positionIds().get(0));
            uploadDocument(session, applicationId, process.requirementIds().get(0), pdf()).andExpect(status().isCreated());

            jdbcTemplate.update("update selection_process set registration_end = now() - interval '1 minute' where id = ?",
                    process.id());
            confirm(session, applicationId).andExpect(status().isConflict());

            advanceProcessStatuses.execute();
            discardExpiredDrafts.execute();

            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from application where id = ?", Integer.class, applicationId)).isZero();
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from application_document where application_id = ?", Integer.class, applicationId))
                    .isZero();
            startRequest(session, process.id(), process.positionIds().get(0)).andExpect(status().isConflict());
        }
    }

    // ---- Auxiliares ----

    private interface IndexedTask {
        int run(int index) throws Exception;
    }

    private static List<Integer> runConcurrently(int count, IndexedTask task) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Integer>> callables = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = i;
            callables.add(() -> {
                start.await();
                return task.run(index);
            });
        }
        List<Integer> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(count)) {
            List<Future<Integer>> futures = callables.stream().map(executor::submit).toList();
            start.countDown();
            for (Future<Integer> future : futures) {
                results.add(future.get());
            }
        }
        return results;
    }

    private Cookie newCandidateSession() throws Exception {
        RegisteredCandidate candidate = auth.registerCandidate();
        return auth.login(candidate.cpf(), candidate.password());
    }

    private ResultActions startRequest(Cookie session, UUID processId, UUID positionId) throws Exception {
        return mockMvc.perform(post("/api/candidate/applications").with(csrf()).cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"processId\": \"%s\", \"positionId\": \"%s\"}".formatted(processId, positionId)));
    }

    private UUID start(Cookie session, UUID processId, UUID positionId) throws Exception {
        return idFrom(startRequest(session, processId, positionId).andExpect(status().isCreated()));
    }

    private ResultActions uploadDocument(Cookie session, UUID applicationId, UUID requirementId, MockMultipartFile file)
            throws Exception {
        return mockMvc.perform(multipart("/api/candidate/applications/{id}/documents", applicationId)
                .file(file).param("requirementId", requirementId.toString()).with(csrf()).cookie(session));
    }

    private ResultActions confirm(Cookie session, UUID applicationId) throws Exception {
        return mockMvc.perform(post("/api/candidate/applications/{id}/confirm", applicationId).with(csrf()).cookie(session));
    }

    private UUID idFrom(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return UUID.fromString(jsonMapper.readTree(body).get("id").asString());
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("file", "documento.pdf", "application/pdf", PDF);
    }
}
