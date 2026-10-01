package br.gov.pmps.processoseletivo.presentation;

import static br.gov.pmps.processoseletivo.support.TestData.loginJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.pmps.processoseletivo.PostgresIntegrationTest;
import br.gov.pmps.processoseletivo.support.AdminTestAccounts;
import br.gov.pmps.processoseletivo.support.AdminTestAccounts.Admin;
import br.gov.pmps.processoseletivo.support.RecordingEmailGateway;
import br.gov.pmps.processoseletivo.support.TestData;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
class AdminManagementTest extends PostgresIntegrationTest {

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

    private AdminTestAccounts admins;
    private Admin manager;
    private Cookie managerSession;

    @BeforeEach
    void setUp() throws Exception {
        admins = new AdminTestAccounts(jdbcTemplate, passwordEncoder, mockMvc);
        manager = admins.create("USUARIO_GERENCIAR", "PERMISSAO_GERENCIAR", "AUDITORIA_VISUALIZAR");
        managerSession = admins.login(manager);
    }

    @Nested
    class Mfa {

        @Test
        void sameCodeCannotBeReused() throws Exception {
            Admin admin = admins.create("PROCESSO_VISUALIZAR");
            String code = AdminTestAccounts.currentCode(admin.mfaSecret());
            verify(passwordStep(admin), code).andExpect(status().isOk());

            verify(passwordStep(admin), code).andExpect(status().isUnauthorized());
        }

        @Test
        void codeWithoutPasswordStepIsRejected() throws Exception {
            Admin admin = admins.create("PROCESSO_VISUALIZAR");

            mockMvc.perform(post("/api/admin/auth/mfa/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\": \"%s\"}".formatted(AdminTestAccounts.currentCode(admin.mfaSecret()))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void wrongCodesLockAccount() throws Exception {
            Admin admin = admins.create("PROCESSO_VISUALIZAR");
            Cookie pending = passwordStep(admin);
            for (int attempt = 0; attempt < 5; attempt++) {
                verify(pending, "000000").andExpect(status().isUnauthorized());
            }

            verify(pending, AdminTestAccounts.currentCode(admin.mfaSecret())).andExpect(status().isUnauthorized());
        }

        @Test
        void resetForcesNewEnrollment() throws Exception {
            Admin admin = admins.create("PROCESSO_VISUALIZAR");
            Cookie adminSession = admins.login(admin);

            operation(admin.accountId(), "reset-mfa").andExpect(status().isNoContent());

            mockMvc.perform(get("/api/auth/session").cookie(adminSession)).andExpect(status().isUnauthorized());
            mockMvc.perform(post("/api/admin/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(admin.cpf(), AdminTestAccounts.PASSWORD)))
                    .andExpect(jsonPath("$.enrollmentRequired").value(true));
        }
    }

    @Nested
    class Administrators {

        @Test
        void createsAdministratorWithoutPasswordAndSendsSetupLink() throws Exception {
            String cpf = TestData.randomCpf();
            String email = "novo-" + TestData.randomEmail();

            create(cpf, email).andExpect(status().isCreated());
            create(cpf, "outro-" + TestData.randomEmail()).andExpect(status().isConflict());

            assertThat(emailGateway.awaitMessageTo(email).body()).contains("#token=");
            mockMvc.perform(get("/api/admin/administrators").cookie(managerSession))
                    .andExpect(jsonPath("$[*].email", hasItem(email)));
        }

        @Test
        void requiresUserManagementPermission() throws Exception {
            Cookie other = admins.sessionWith("PROCESSO_VISUALIZAR");

            mockMvc.perform(get("/api/admin/administrators").cookie(other)).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/admin/audit").cookie(other)).andExpect(status().isForbidden());
        }

        @Test
        void deactivationEndsSessionsAndBlocksLogin() throws Exception {
            Admin admin = admins.create("PROCESSO_VISUALIZAR");
            Cookie adminSession = admins.login(admin);

            operation(admin.accountId(), "deactivate").andExpect(status().isNoContent());

            mockMvc.perform(get("/api/auth/session").cookie(adminSession)).andExpect(status().isUnauthorized());
            mockMvc.perform(post("/api/admin/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(admin.cpf(), AdminTestAccounts.PASSWORD)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void cannotDeactivateOrChangeOwnAccount() throws Exception {
            operation(manager.accountId(), "deactivate").andExpect(status().isConflict());
            replaceRoles(manager.accountId(), "[]").andExpect(status().isConflict());
        }

        @Test
        void roleChangeAppliesNewPermissionsAfterNewLogin() throws Exception {
            Admin admin = admins.create("PROCESSO_VISUALIZAR");
            Cookie adminSession = admins.login(admin);
            String roleCode = createRole("Auditores " + UUID.randomUUID(), "[\"AUDITORIA_VISUALIZAR\"]");

            replaceRoles(admin.accountId(), "[\"%s\"]".formatted(roleCode)).andExpect(status().isNoContent());

            mockMvc.perform(get("/api/auth/session").cookie(adminSession)).andExpect(status().isUnauthorized());
            Cookie renewed = admins.login(admin);
            mockMvc.perform(get("/api/admin/audit").cookie(renewed)).andExpect(status().isOk());
            mockMvc.perform(get("/api/admin/processes").cookie(renewed)).andExpect(status().isForbidden());
        }
    }

    @Nested
    class Roles {

        @Test
        void systemRoleCannotBeChanged() throws Exception {
            mockMvc.perform(put("/api/admin/roles/ADMINISTRADOR_GERAL").with(csrf()).cookie(managerSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"Outro\", \"permissions\": []}"))
                    .andExpect(status().isConflict());
        }

        @Test
        void validatesPermissionsAndBlocksDeletionWithMembers() throws Exception {
            mockMvc.perform(post("/api/admin/roles").with(csrf()).cookie(managerSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": \"Invalido %s\", \"permissions\": [\"NAO_EXISTE\"]}".formatted(UUID.randomUUID())))
                    .andExpect(status().isBadRequest());

            String roleCode = createRole("Consulta " + UUID.randomUUID(), "[\"PROCESSO_VISUALIZAR\"]");
            Admin member = admins.create();
            replaceRoles(member.accountId(), "[\"%s\"]".formatted(roleCode)).andExpect(status().isNoContent());

            mockMvc.perform(delete("/api/admin/roles/{code}", roleCode).with(csrf()).cookie(managerSession))
                    .andExpect(status().isConflict());
            replaceRoles(member.accountId(), "[]").andExpect(status().isNoContent());
            mockMvc.perform(delete("/api/admin/roles/{code}", roleCode).with(csrf()).cookie(managerSession))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    class Audit {

        @Test
        void filtersByActionAndTarget() throws Exception {
            Admin admin = admins.create("PROCESSO_VISUALIZAR");
            operation(admin.accountId(), "deactivate").andExpect(status().isNoContent());

            mockMvc.perform(get("/api/admin/audit").cookie(managerSession)
                            .param("action", "ADMINISTRATOR_DEACTIVATED")
                            .param("targetId", admin.accountId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].actorName").value("Administrador de Teste"));
        }

        @Test
        void filterValuesAreNeverConcatenatedIntoSql() throws Exception {
            mockMvc.perform(get("/api/admin/audit").cookie(managerSession)
                            .param("action", "X' OR '1'='1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    private Cookie passwordStep(Admin admin) throws Exception {
        return mockMvc.perform(post("/api/admin/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(admin.cpf(), AdminTestAccounts.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("SESSION");
    }

    private ResultActions verify(Cookie pending, String code) throws Exception {
        return mockMvc.perform(post("/api/admin/auth/mfa/verify").with(csrf()).cookie(pending)
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\": \"%s\"}".formatted(code)));
    }

    private ResultActions create(String cpf, String email) throws Exception {
        return mockMvc.perform(post("/api/admin/administrators").with(csrf()).cookie(managerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cpf\": \"%s\", \"fullName\": \"Servidor Teste\", \"email\": \"%s\"}".formatted(cpf, email)));
    }

    private ResultActions operation(UUID accountId, String operation) throws Exception {
        return mockMvc.perform(post("/api/admin/administrators/{id}/" + operation, accountId)
                .with(csrf()).cookie(managerSession));
    }

    private ResultActions replaceRoles(UUID accountId, String rolesJson) throws Exception {
        return mockMvc.perform(put("/api/admin/administrators/{id}/roles", accountId).with(csrf()).cookie(managerSession)
                .contentType(MediaType.APPLICATION_JSON).content("{\"roleCodes\": %s}".formatted(rolesJson)));
    }

    private String createRole(String name, String permissionsJson) throws Exception {
        String body = mockMvc.perform(post("/api/admin/roles").with(csrf()).cookie(managerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\", \"permissions\": %s}".formatted(name, permissionsJson)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(body).get("code").asString();
    }
}
