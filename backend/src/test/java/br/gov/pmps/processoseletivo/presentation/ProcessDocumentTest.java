package br.gov.pmps.processoseletivo.presentation;

import static br.gov.pmps.processoseletivo.support.ProcessFixtures.PDF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.pmps.processoseletivo.PostgresIntegrationTest;
import br.gov.pmps.processoseletivo.support.AdminTestAccounts;
import br.gov.pmps.processoseletivo.support.ProcessFixtures;
import br.gov.pmps.processoseletivo.support.ProcessFixtures.CreatedProcess;
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
class ProcessDocumentTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

    private AdminTestAccounts admins;
    private Cookie publisher;
    private CreatedProcess process;

    @BeforeEach
    void setUp() throws Exception {
        admins = new AdminTestAccounts(jdbcTemplate, passwordEncoder, mockMvc);
        Cookie processAdmin = admins.sessionWith(
                "PROCESSO_VISUALIZAR", "PROCESSO_CRIAR", "PROCESSO_EDITAR", "PROCESSO_PUBLICAR", "PROCESSO_ENCERRAR");
        publisher = admins.sessionWith("PROCESSO_VISUALIZAR", "RESULTADO_PUBLICAR");
        process = new ProcessFixtures(mockMvc, jsonMapper, processAdmin).openProcess(false, 1);
    }

    @Test
    void publishedDocumentAppearsPubliclyWithAdministratorDefinedName() throws Exception {
        UUID documentId = idFrom(publish(publisher, "Relação de inscrições deferidas", pdf()));

        mockMvc.perform(get("/api/processes/{id}/documents", process.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Relação de inscrições deferidas"));
        mockMvc.perform(get("/api/processes/{id}/documents/{doc}/file", process.id(), documentId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void withdrawalRequiresReasonHidesPubliclyAndKeepsRecord() throws Exception {
        UUID documentId = idFrom(publish(publisher, "Comunicado", pdf()));

        withdraw(documentId, "").andExpect(status().isBadRequest());
        withdraw(documentId, "Publicado por engano").andExpect(status().isNoContent());
        withdraw(documentId, "De novo").andExpect(status().isConflict());

        mockMvc.perform(get("/api/processes/{id}/documents", process.id())).andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/processes/{id}/documents/{doc}/file", process.id(), documentId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/admin/processes/{id}/documents", process.id()).cookie(publisher))
                .andExpect(jsonPath("$[0].withdrawalReason").value("Publicado por engano"));
        assertThatThrownBy(() -> jdbcTemplate.update("delete from process_document where id = ?", documentId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void requiresNameAndPdfAndPermission() throws Exception {
        publish(publisher, " ", pdf()).andExpect(status().isBadRequest());
        publish(publisher, "Imagem", new MockMultipartFile("file", "foto.png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})).andExpect(status().isBadRequest());

        Cookie viewer = admins.sessionWith("PROCESSO_VISUALIZAR");
        publish(viewer, "Resultado", pdf()).andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/admin/processes/{id}/documents", process.id()).file(pdf())
                        .param("name", "Resultado").with(csrf()))
                .andExpect(status().isUnauthorized());
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from process_document where process_id = ?", Integer.class, process.id())).isZero();
    }

    private ResultActions publish(Cookie session, String name, MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/admin/processes/{id}/documents", process.id())
                .file(file).param("name", name).with(csrf()).cookie(session));
    }

    private ResultActions withdraw(UUID documentId, String reason) throws Exception {
        return mockMvc.perform(post("/api/admin/processes/{id}/documents/{doc}/withdraw", process.id(), documentId)
                .with(csrf()).cookie(publisher).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"%s\"}".formatted(reason)));
    }

    private UUID idFrom(ResultActions result) throws Exception {
        String body = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(jsonMapper.readTree(body).get("id").asString());
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("file", "documento.pdf", "application/pdf", PDF);
    }
}
