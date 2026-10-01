package br.gov.pmps.processoseletivo.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.pmps.processoseletivo.PostgresIntegrationTest;
import br.gov.pmps.processoseletivo.application.usecase.process.AdvanceProcessStatusesUseCase;
import br.gov.pmps.processoseletivo.support.AdminTestAccounts;
import br.gov.pmps.processoseletivo.support.AuthTestClient;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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
class ProcessManagementTest extends PostgresIntegrationTest {

    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    private static final String[] ALL_PROCESS_PERMISSIONS = {
        "PROCESSO_VISUALIZAR", "PROCESSO_CRIAR", "PROCESSO_EDITAR", "PROCESSO_PUBLICAR", "PROCESSO_ENCERRAR"
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private AdvanceProcessStatusesUseCase advanceProcessStatuses;

    private Cookie admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = new AdminTestAccounts(jdbcTemplate, passwordEncoder, mockMvc).sessionWith(ALL_PROCESS_PERMISSIONS);
    }

    @Nested
    class Authorization {

        @Test
        void anonymousCannotUseAdministrativeEndpoints() throws Exception {
            mockMvc.perform(get("/api/admin/processes")).andExpect(status().isUnauthorized());
        }

        @Test
        void candidateCannotUseAdministrativeEndpoints() throws Exception {
            AuthTestClient auth = new AuthTestClient(mockMvc);
            AuthTestClient.RegisteredCandidate candidate = auth.registerCandidate();

            mockMvc.perform(get("/api/admin/processes").cookie(auth.login(candidate.cpf(), candidate.password())))
                    .andExpect(status().isForbidden());
        }

        @Test
        void administratorNeedsSpecificPermissionForEachOperation() throws Exception {
            Cookie viewer = new AdminTestAccounts(jdbcTemplate, passwordEncoder, mockMvc)
                    .sessionWith("PROCESSO_VISUALIZAR");
            UUID processId = createProcess(admin, futureStart(), futureEnd());

            mockMvc.perform(get("/api/admin/processes").cookie(viewer)).andExpect(status().isOk());
            mockMvc.perform(post("/api/admin/processes").with(csrf()).cookie(viewer)
                            .contentType(MediaType.APPLICATION_JSON).content(processJson(uniqueNumber(), futureStart(), futureEnd())))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/admin/processes/{id}/publish", processId).with(csrf()).cookie(viewer))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/admin/processes/{id}/cancel", processId).with(csrf()).cookie(viewer)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"teste\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class DraftAndPublication {

        @Test
        void fullFlowFromDraftToPublicNotice() throws Exception {
            UUID processId = createProcess(admin, futureStart(), futureEnd());
            addPosition(processId, "Auxiliar Administrativo", 10).andExpect(status().isCreated());
            mockMvc.perform(post("/api/admin/processes/{id}/document-requirements", processId).with(csrf()).cookie(admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"Diploma\", \"description\": null, \"mandatory\": true, \"title\": true}"))
                    .andExpect(status().isCreated());
            uploadNotice(processId, pdf("edital.pdf"), null).andExpect(status().isNoContent());

            // Rascunho não aparece publicamente.
            mockMvc.perform(get("/api/processes/{id}", processId)).andExpect(status().isNotFound());
            mockMvc.perform(get("/api/processes/{id}/notices/1/file", processId)).andExpect(status().isNotFound());

            publish(processId).andExpect(status().isNoContent());

            mockMvc.perform(get("/api/processes/{id}", processId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PUBLICADO"))
                    .andExpect(jsonPath("$.positions[0].name").value("Auxiliar Administrativo"))
                    .andExpect(jsonPath("$.positions[0].vacancies").value(10))
                    .andExpect(jsonPath("$.documentRequirements[0].title").value(true))
                    .andExpect(jsonPath("$.notices[0].version").value(1));
            mockMvc.perform(get("/api/processes/{id}/notices/1/file", processId))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/pdf"))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")))
                    .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("sandbox")));

            Integer audits = jdbcTemplate.queryForObject(
                    "select count(*) from audit_log where target_id = ? and action in ('PROCESS_CREATED', 'PROCESS_STATUS_CHANGED')",
                    Integer.class, processId.toString());
            assertThat(audits).isGreaterThanOrEqualTo(2);
        }

        @Test
        void publishRequiresPositionAndNotice() throws Exception {
            UUID processId = createProcess(admin, futureStart(), futureEnd());
            publish(processId).andExpect(status().isConflict());

            addPosition(processId, "Cargo", 1).andExpect(status().isCreated());
            publish(processId).andExpect(status().isConflict());

            uploadNotice(processId, pdf("edital.pdf"), null).andExpect(status().isNoContent());
            publish(processId).andExpect(status().isNoContent());
        }

        @Test
        void publishedProcessCannotBeEdited() throws Exception {
            UUID processId = publishedProcess(futureStart(), futureEnd());

            addPosition(processId, "Novo cargo", 1).andExpect(status().isConflict());
            mockMvc.perform(put("/api/admin/processes/{id}", processId).with(csrf()).cookie(admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(processJson(uniqueNumber(), futureStart(), futureEnd())))
                    .andExpect(status().isConflict());
        }

        @Test
        void rejectsDuplicateNumberAndYear() throws Exception {
            String number = uniqueNumber();
            createProcess(admin, number, futureStart(), futureEnd());

            mockMvc.perform(post("/api/admin/processes").with(csrf()).cookie(admin)
                            .contentType(MediaType.APPLICATION_JSON).content(processJson(number, futureStart(), futureEnd())))
                    .andExpect(status().isConflict());
        }

        @Test
        void positionFromAnotherProcessIsNotFound() throws Exception {
            UUID first = createProcess(admin, futureStart(), futureEnd());
            UUID second = createProcess(admin, futureStart(), futureEnd());
            UUID positionOfFirst = idFrom(addPosition(first, "Cargo", 1));

            mockMvc.perform(delete("/api/admin/processes/{id}/positions/{positionId}", second, positionOfFirst)
                            .with(csrf()).cookie(admin))
                    .andExpect(status().isConflict());
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from process_position where id = ?", Integer.class, positionOfFirst)).isEqualTo(1);
        }

        @Test
        void draftsNeverAppearInPublicList() throws Exception {
            createProcess(admin, futureStart(), futureEnd());

            mockMvc.perform(get("/api/processes").param("status", "RASCUNHO").param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
            String body = mockMvc.perform(get("/api/processes").param("size", "50"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContain("\"RASCUNHO\"");
        }
    }

    @Nested
    class NoticeFiles {

        @Test
        void rejectsContentThatIsNotPdf() throws Exception {
            UUID processId = createProcess(admin, futureStart(), futureEnd());

            uploadNotice(processId, new MockMultipartFile("file", "edital.pdf", "application/pdf",
                    "<script>alert(1)</script>".getBytes()), null)
                    .andExpect(status().isBadRequest());
            uploadNotice(processId, new MockMultipartFile("file", "edital.exe", "application/pdf", PDF), null)
                    .andExpect(status().isBadRequest());
            uploadNotice(processId, new MockMultipartFile("file", "edital.pdf", "text/html", PDF), null)
                    .andExpect(status().isBadRequest());
            uploadNotice(processId, new MockMultipartFile("file", "imagem.png", "image/png",
                    new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}), null)
                    .andExpect(status().isBadRequest());
        }

        @Test
        void storesFileUnderRandomNameKeepingOriginalOnlyAsMetadata() throws Exception {
            UUID processId = createProcess(admin, futureStart(), futureEnd());

            uploadNotice(processId, pdf("../../etc/edital.pdf"), null).andExpect(status().isNoContent());

            var row = jdbcTemplate.queryForMap("""
                    select f.storage_key, f.original_name from stored_file f
                      join process_notice n on n.file_id = f.id where n.process_id = ?
                    """, processId);
            assertThat((String) row.get("storage_key")).matches("[0-9a-f]{64}");
            assertThat(row.get("original_name")).isEqualTo("edital.pdf");
        }

        @Test
        void rectificationRequiresReasonAndKeepsPreviousVersion() throws Exception {
            UUID processId = publishedProcess(futureStart(), futureEnd());

            uploadNotice(processId, pdf("retificacao.pdf"), null).andExpect(status().isBadRequest());
            uploadNotice(processId, pdf("retificacao.pdf"), "Correção do cronograma").andExpect(status().isNoContent());

            mockMvc.perform(get("/api/processes/{id}", processId))
                    .andExpect(jsonPath("$.notices[0].version").value(2))
                    .andExpect(jsonPath("$.notices[0].status").value("CURRENT"))
                    .andExpect(jsonPath("$.notices[0].changeReason").value("Correção do cronograma"))
                    .andExpect(jsonPath("$.notices[1].version").value(1))
                    .andExpect(jsonPath("$.notices[1].status").value("SUPERSEDED"));
            mockMvc.perform(get("/api/processes/{id}/notices/1/file", processId)).andExpect(status().isOk());
            mockMvc.perform(get("/api/processes/{id}/notices/2/file", processId)).andExpect(status().isOk());
        }
    }

    @Nested
    class Lifecycle {

        @Test
        void publishingAfterStartOpensRegistrationImmediately() throws Exception {
            UUID processId = publishedProcess(Instant.now().minus(Duration.ofHours(1)), futureEnd());

            assertThat(statusOf(processId)).isEqualTo("INSCRICOES_ABERTAS");
        }

        @Test
        void scheduledTransitionClosesRegistrationAndRecordsHistory() throws Exception {
            UUID processId = publishedProcess(Instant.now().minus(Duration.ofHours(1)), futureEnd());
            jdbcTemplate.update("update selection_process set registration_end = now() - interval '1 minute' where id = ?",
                    processId);

            advanceProcessStatuses.execute();

            assertThat(statusOf(processId)).isEqualTo("INSCRICOES_ENCERRADAS");
            mockMvc.perform(get("/api/admin/processes/{id}/history", processId).cookie(admin))
                    .andExpect(jsonPath("$[0].toStatus").value("RASCUNHO"))
                    .andExpect(jsonPath("$[1].toStatus").value("PUBLICADO"))
                    .andExpect(jsonPath("$[1].changedBy").value("Administrador de Teste"))
                    .andExpect(jsonPath("$[2].toStatus").value("INSCRICOES_ABERTAS"))
                    .andExpect(jsonPath("$[3].toStatus").value("INSCRICOES_ENCERRADAS"))
                    .andExpect(jsonPath("$[3].changedBy").isEmpty());
        }

        @Test
        void extensionOnlyBeforeClosingAndWithReason() throws Exception {
            UUID processId = publishedProcess(Instant.now().minus(Duration.ofHours(1)), futureEnd());

            extend(processId, futureEnd().plus(Duration.ofDays(5)), "").andExpect(status().isBadRequest());
            extend(processId, futureEnd().minus(Duration.ofDays(1)), "Prorrogação").andExpect(status().isConflict());
            extend(processId, futureEnd().plus(Duration.ofDays(5)), "Prorrogação por decreto")
                    .andExpect(status().isNoContent());

            jdbcTemplate.update("update selection_process set registration_end = now() - interval '1 minute' where id = ?",
                    processId);
            advanceProcessStatuses.execute();
            extend(processId, futureEnd().plus(Duration.ofDays(30)), "Reabrir").andExpect(status().isConflict());
        }

        @Test
        void suspendResumeAndCancel() throws Exception {
            UUID processId = publishedProcess(futureStart(), futureEnd());

            lifecycle(processId, "suspend", "Decisão judicial").andExpect(status().isNoContent());
            assertThat(statusOf(processId)).isEqualTo("SUSPENSO");
            lifecycle(processId, "resume", "Decisão revogada").andExpect(status().isNoContent());
            assertThat(statusOf(processId)).isEqualTo("PUBLICADO");

            lifecycle(processId, "cancel", "Cancelamento administrativo").andExpect(status().isNoContent());
            lifecycle(processId, "cancel", "De novo").andExpect(status().isConflict());
            lifecycle(processId, "resume", "Tentativa").andExpect(status().isConflict());
            uploadNotice(processId, pdf("novo.pdf"), "Retificação").andExpect(status().isConflict());
        }

        @Test
        void suspensionRequiresReason() throws Exception {
            UUID processId = publishedProcess(futureStart(), futureEnd());

            lifecycle(processId, "suspend", " ").andExpect(status().isBadRequest());
        }

        @Test
        void archiveRequiresFinalResult() throws Exception {
            UUID processId = publishedProcess(futureStart(), futureEnd());

            lifecycle(processId, "archive", "Encerrado").andExpect(status().isConflict());
        }
    }

    // ---- Auxiliares ----

    private UUID publishedProcess(Instant start, Instant end) throws Exception {
        UUID processId = createProcess(admin, start, end);
        addPosition(processId, "Cargo Teste", 3).andExpect(status().isCreated());
        uploadNotice(processId, pdf("edital.pdf"), null).andExpect(status().isNoContent());
        publish(processId).andExpect(status().isNoContent());
        return processId;
    }

    private UUID createProcess(Cookie session, Instant start, Instant end) throws Exception {
        return createProcess(session, uniqueNumber(), start, end);
    }

    private UUID createProcess(Cookie session, String number, Instant start, Instant end) throws Exception {
        return idFrom(mockMvc.perform(post("/api/admin/processes").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(processJson(number, start, end)))
                .andExpect(status().isCreated()));
    }

    private ResultActions addPosition(UUID processId, String name, int vacancies) throws Exception {
        return mockMvc.perform(post("/api/admin/processes/{id}/positions", processId).with(csrf()).cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"%s\", \"vacancies\": %d}".formatted(name, vacancies)));
    }

    private ResultActions uploadNotice(UUID processId, MockMultipartFile file, String reason) throws Exception {
        var request = multipart("/api/admin/processes/{id}/notices", processId).file(file).with(csrf()).cookie(admin);
        if (reason != null) {
            request.param("reason", reason);
        }
        return mockMvc.perform(request);
    }

    private ResultActions publish(UUID processId) throws Exception {
        return mockMvc.perform(post("/api/admin/processes/{id}/publish", processId).with(csrf()).cookie(admin));
    }

    private ResultActions extend(UUID processId, Instant newEnd, String reason) throws Exception {
        return mockMvc.perform(post("/api/admin/processes/{id}/extend-registration", processId).with(csrf()).cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newRegistrationEnd\": \"%s\", \"reason\": \"%s\"}".formatted(newEnd, reason)));
    }

    private ResultActions lifecycle(UUID processId, String operation, String reason) throws Exception {
        return mockMvc.perform(post("/api/admin/processes/{id}/" + operation, processId).with(csrf()).cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"%s\"}".formatted(reason)));
    }

    private String statusOf(UUID processId) {
        return jdbcTemplate.queryForObject("select status from selection_process where id = ?", String.class, processId);
    }

    private UUID idFrom(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return UUID.fromString(jsonMapper.readTree(body).get("id").asString());
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", PDF);
    }

    private static String processJson(String number, Instant start, Instant end) {
        return """
                {
                  "number": "%s",
                  "year": 2026,
                  "title": "Processo Seletivo de Teste",
                  "department": "Secretaria de Administração",
                  "registrationStart": "%s",
                  "registrationEnd": "%s",
                  "multipleApplicationsAllowed": false,
                  "titleEvaluationEnabled": true
                }
                """.formatted(number, start, end);
    }

    private static String uniqueNumber() {
        return "T" + ThreadLocalRandom.current().nextInt(1, 999_999_999);
    }

    private static Instant futureStart() {
        return Instant.now().plus(Duration.ofDays(1));
    }

    private static Instant futureEnd() {
        return Instant.now().plus(Duration.ofDays(10)).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    }
}
