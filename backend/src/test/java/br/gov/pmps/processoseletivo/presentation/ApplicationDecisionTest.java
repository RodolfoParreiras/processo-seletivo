package br.gov.pmps.processoseletivo.presentation;

import static br.gov.pmps.processoseletivo.support.ProcessFixtures.PDF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.pmps.processoseletivo.PostgresIntegrationTest;
import br.gov.pmps.processoseletivo.application.usecase.process.AdvanceProcessStatusesUseCase;
import br.gov.pmps.processoseletivo.support.AdminTestAccounts;
import br.gov.pmps.processoseletivo.support.AuthTestClient;
import br.gov.pmps.processoseletivo.support.AuthTestClient.RegisteredCandidate;
import br.gov.pmps.processoseletivo.support.ProcessFixtures;
import br.gov.pmps.processoseletivo.support.ProcessFixtures.CreatedProcess;
import br.gov.pmps.processoseletivo.support.ProcessFixtures.Requirement;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
class ApplicationDecisionTest extends PostgresIntegrationTest {

    private static final String[] PROCESS_PERMISSIONS = {
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

    private AdminTestAccounts admins;
    private Cookie decider;
    private CreatedProcess process;
    private Cookie candidateSession;
    private UUID applicationId;
    private UUID documentId;

    @BeforeEach
    void setUp() throws Exception {
        admins = new AdminTestAccounts(jdbcTemplate, passwordEncoder, mockMvc);
        Cookie processAdmin = admins.sessionWith(PROCESS_PERMISSIONS);
        decider = admins.sessionWith("INSCRICAO_VISUALIZAR", "INSCRICAO_DEFERIR", "INSCRICAO_INDEFERIR");
        process = new ProcessFixtures(mockMvc, jsonMapper, processAdmin)
                .openProcess(false, 1, new Requirement("Documento de identidade", true));

        AuthTestClient auth = new AuthTestClient(mockMvc);
        RegisteredCandidate candidate = auth.registerCandidate();
        candidateSession = auth.login(candidate.cpf(), candidate.password());
        applicationId = idFrom(mockMvc.perform(post("/api/candidate/applications").with(csrf()).cookie(candidateSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"processId\": \"%s\", \"positionId\": \"%s\"}"
                        .formatted(process.id(), process.positionIds().get(0)))));
        documentId = idFrom(mockMvc.perform(multipart("/api/candidate/applications/{id}/documents", applicationId)
                .file(new MockMultipartFile("file", "rg.pdf", "application/pdf", PDF))
                .param("requirementId", process.requirementIds().get(0).toString())
                .with(csrf()).cookie(candidateSession)));
        mockMvc.perform(post("/api/candidate/applications/{id}/confirm", applicationId).with(csrf()).cookie(candidateSession))
                .andExpect(status().isOk());
    }

    @Test
    void decisionsOnlyAfterRegistrationCloses() throws Exception {
        deny(decider, "Documento ilegível").andExpect(status().isConflict());

        closeRegistrations();
        deny(decider, "Documento ilegível").andExpect(status().isNoContent());
    }

    @Test
    void denialRequiresReasonAndIsShownToCandidate() throws Exception {
        closeRegistrations();

        deny(decider, " ").andExpect(status().isConflict());
        deny(decider, "Documento de identidade ilegível").andExpect(status().isNoContent());

        mockMvc.perform(get("/api/candidate/applications/{id}", applicationId).cookie(candidateSession))
                .andExpect(jsonPath("$.status").value("INDEFERIDA"))
                .andExpect(jsonPath("$.decisionReason").value("Documento de identidade ilegível"));
    }

    @Test
    void revisionAfterAppealRequiresReasonAndKeepsHistory() throws Exception {
        closeRegistrations();
        deny(decider, "Documento ilegível").andExpect(status().isNoContent());

        defer(decider, null).andExpect(status().isConflict());
        defer(decider, "Recurso deferido: documento legível reenviado por e-mail").andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/applications/{id}", applicationId).cookie(decider))
                .andExpect(jsonPath("$.status").value("DEFERIDA"))
                .andExpect(jsonPath("$.decisions[0].toStatus").value("INDEFERIDA"))
                .andExpect(jsonPath("$.decisions[0].decidedBy").value("Administrador de Teste"))
                .andExpect(jsonPath("$.decisions[1].fromStatus").value("INDEFERIDA"))
                .andExpect(jsonPath("$.decisions[1].toStatus").value("DEFERIDA"))
                .andExpect(jsonPath("$.decisions[1].reason").value("Recurso deferido: documento legível reenviado por e-mail"));
        assertThatThrownBy(() -> jdbcTemplate.update(
                "update application_decision set reason = 'alterado' where application_id = ?", applicationId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void initialDeferralDoesNotRequireReason() throws Exception {
        closeRegistrations();

        defer(decider, null).andExpect(status().isNoContent());
        defer(decider, null).andExpect(status().isConflict());
    }

    @Test
    void decisionsBlockedAfterFinalResult() throws Exception {
        closeRegistrations();
        jdbcTemplate.update("update selection_process set status = 'RESULTADO_DEFINITIVO' where id = ?", process.id());

        defer(decider, null).andExpect(status().isConflict());
    }

    @Test
    void eachDecisionRequiresItsPermission() throws Exception {
        closeRegistrations();
        Cookie viewer = admins.sessionWith("INSCRICAO_VISUALIZAR");
        Cookie deferOnly = admins.sessionWith("INSCRICAO_VISUALIZAR", "INSCRICAO_DEFERIR");

        mockMvc.perform(get("/api/admin/applications/{id}", applicationId).cookie(viewer)).andExpect(status().isOk());
        defer(viewer, null).andExpect(status().isForbidden());
        deny(deferOnly, "Motivo").andExpect(status().isForbidden());
        defer(deferOnly, null).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/applications/{id}", applicationId).cookie(candidateSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabilityDataRequiresSpecificPermissionAndIsAudited() throws Exception {
        Cookie withPcdPermission = admins.sessionWith("INSCRICAO_VISUALIZAR", "DADOS_PCD_VISUALIZAR");

        mockMvc.perform(get("/api/admin/applications/{id}", applicationId).cookie(decider))
                .andExpect(jsonPath("$.candidate.fullName").value("Maria Teste Silva"))
                .andExpect(jsonPath("$.candidate.hasDisability").isEmpty())
                .andExpect(jsonPath("$.candidate.adaptations").isEmpty());
        mockMvc.perform(get("/api/admin/applications/{id}", applicationId).cookie(withPcdPermission))
                .andExpect(jsonPath("$.candidate.hasDisability").value(false));
    }

    @Test
    void documentDownloadByAdministratorIsAudited() throws Exception {
        mockMvc.perform(get("/api/admin/applications/{id}/documents/{doc}/file", applicationId, documentId)
                        .cookie(decider))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_log where action = 'APPLICATION_DOCUMENT_ACCESSED' and target_id = ?",
                Integer.class, applicationId.toString())).isEqualTo(1);
    }

    @Test
    void listShowsConfirmedApplicationsOnly() throws Exception {
        AuthTestClient auth = new AuthTestClient(mockMvc);
        RegisteredCandidate other = auth.registerCandidate();
        Cookie otherSession = auth.login(other.cpf(), other.password());
        mockMvc.perform(post("/api/candidate/applications").with(csrf()).cookie(otherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processId\": \"%s\", \"positionId\": \"%s\"}"
                                .formatted(process.id(), process.positionIds().get(0))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/processes/{id}/applications", process.id()).cookie(decider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].candidateName").value("Maria Teste Silva"))
                .andExpect(jsonPath("$.content[0].status").value("RECEBIDA"));
        mockMvc.perform(get("/api/admin/processes/{id}/applications", process.id()).param("status", "DEFERIDA")
                        .cookie(decider))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private void closeRegistrations() {
        jdbcTemplate.update("update selection_process set registration_end = now() - interval '1 minute' where id = ?",
                process.id());
        advanceProcessStatuses.execute();
    }

    private ResultActions defer(Cookie session, String reason) throws Exception {
        return decision(session, "defer", reason);
    }

    private ResultActions deny(Cookie session, String reason) throws Exception {
        return decision(session, "deny", reason);
    }

    private ResultActions decision(Cookie session, String operation, String reason) throws Exception {
        String body = reason == null ? "{}" : "{\"reason\": \"%s\"}".formatted(reason);
        return mockMvc.perform(post("/api/admin/applications/{id}/" + operation, applicationId).with(csrf()).cookie(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private UUID idFrom(ResultActions result) throws Exception {
        String body = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(jsonMapper.readTree(body).get("id").asString());
    }
}
