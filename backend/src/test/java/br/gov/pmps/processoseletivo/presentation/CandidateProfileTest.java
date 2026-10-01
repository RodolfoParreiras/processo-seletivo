package br.gov.pmps.processoseletivo.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.pmps.processoseletivo.PostgresIntegrationTest;
import br.gov.pmps.processoseletivo.support.AuthTestClient;
import br.gov.pmps.processoseletivo.support.AuthTestClient.RegisteredCandidate;
import br.gov.pmps.processoseletivo.support.RecordingEmailGateway;
import br.gov.pmps.processoseletivo.support.TestData;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@AutoConfigureMockMvc
class CandidateProfileTest extends PostgresIntegrationTest {

    private static final String UPDATED_PROFILE = """
            {
              "fullName": "Ana Atualizada Souza",
              "birthDate": "1991-07-20",
              "motherName": "Joana Teste Silva",
              "phone": "2433330000",
              "cep": "25850100",
              "street": "Avenida Nova",
              "addressNumber": "200",
              "complement": "Apto 1",
              "neighborhood": "Centro",
              "city": "Paraíba do Sul",
              "uf": "RJ",
              "hasDisability": true,
              "adaptations": ["READING_ASSISTANCE"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingEmailGateway emailGateway;

    private AuthTestClient auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestClient(mockMvc);
    }

    @Nested
    class Access {

        @Test
        void requiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/candidate/me")).andExpect(status().isUnauthorized());
        }

        @Test
        void administratorsCannotUseCandidateEndpoints() throws Exception {
            mockMvc.perform(get("/api/candidate/me").with(user("admin").roles("ADMIN")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void eachCandidateSeesOnlyOwnData() throws Exception {
            RegisteredCandidate first = auth.registerCandidate();
            RegisteredCandidate second = auth.registerCandidate();

            mockMvc.perform(get("/api/candidate/me").cookie(auth.login(first.cpf(), first.password())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cpf").value(first.cpf()))
                    .andExpect(jsonPath("$.email").value(first.email()));
            mockMvc.perform(get("/api/candidate/me").cookie(auth.login(second.cpf(), second.password())))
                    .andExpect(jsonPath("$.cpf").value(second.cpf()));
        }

        @Test
        void responseDoesNotExposeInternalFields() throws Exception {
            RegisteredCandidate candidate = auth.registerCandidate();

            String body = mockMvc.perform(get("/api/candidate/me")
                            .cookie(auth.login(candidate.cpf(), candidate.password())))
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).doesNotContain("passwordHash", "argon2", "userAccountId", "failedLoginAttempts", "\"id\"");
        }
    }

    @Nested
    class UpdateProfile {

        @Test
        void updatesDataAuditsOnlyFieldNamesAndRefreshesSessionName() throws Exception {
            RegisteredCandidate candidate = auth.registerCandidate();
            Cookie session = auth.login(candidate.cpf(), candidate.password());

            mockMvc.perform(put("/api/candidate/me").with(csrf()).cookie(session)
                            .contentType(MediaType.APPLICATION_JSON).content(UPDATED_PROFILE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("Ana Atualizada Souza"))
                    .andExpect(jsonPath("$.complement").value("Apto 1"))
                    .andExpect(jsonPath("$.adaptations[0]").value("READING_ASSISTANCE"));

            mockMvc.perform(get("/api/auth/session").cookie(session))
                    .andExpect(jsonPath("$.displayName").value("Ana Atualizada Souza"));

            String details = jdbcTemplate.queryForObject("""
                    select l.details::text from audit_log l join user_account a on a.id = l.actor_user_account_id
                     where a.cpf = ? and l.action = 'CANDIDATE_DATA_UPDATED'
                    """, String.class, candidate.cpf());
            assertThat(details).contains("fullName", "address", "adaptations")
                    .doesNotContain("Ana Atualizada", "Avenida Nova", "2433330000");
        }

        @Test
        void cpfCannotBeChangedThroughProfile() throws Exception {
            RegisteredCandidate candidate = auth.registerCandidate();
            Cookie session = auth.login(candidate.cpf(), candidate.password());
            String withCpf = UPDATED_PROFILE.replace("{", "{\"cpf\": \"" + TestData.randomCpf() + "\",");

            mockMvc.perform(put("/api/candidate/me").with(csrf()).cookie(session)
                            .contentType(MediaType.APPLICATION_JSON).content(withCpf))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cpf").value(candidate.cpf()));
        }

        @Test
        void rejectsNoneCombinedWithOtherAdaptations() throws Exception {
            RegisteredCandidate candidate = auth.registerCandidate();
            Cookie session = auth.login(candidate.cpf(), candidate.password());

            mockMvc.perform(put("/api/candidate/me").with(csrf()).cookie(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATED_PROFILE.replace("[\"READING_ASSISTANCE\"]",
                                    "[\"READING_ASSISTANCE\", \"NONE\"]")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void requiresCsrfToken() throws Exception {
            RegisteredCandidate candidate = auth.registerCandidate();
            Cookie session = auth.login(candidate.cpf(), candidate.password());

            mockMvc.perform(put("/api/candidate/me").cookie(session)
                            .contentType(MediaType.APPLICATION_JSON).content(UPDATED_PROFILE))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ChangeEmail {

        @Test
        void requiresCurrentPassword() throws Exception {
            RegisteredCandidate candidate = auth.registerCandidate();
            Cookie session = auth.login(candidate.cpf(), candidate.password());

            changeEmail(session, TestData.randomEmail(), "Errada#2024x").andExpect(status().isBadRequest());
        }

        @Test
        void changesEmailAndNotifiesPreviousAddress() throws Exception {
            RegisteredCandidate candidate = auth.registerCandidate();
            Cookie session = auth.login(candidate.cpf(), candidate.password());
            String newEmail = TestData.randomEmail();

            changeEmail(session, newEmail, candidate.password()).andExpect(status().isNoContent());

            assertThat(emailGateway.awaitMessageTo(candidate.email()).subject()).contains("E-mail da conta alterado");
            mockMvc.perform(get("/api/candidate/me").cookie(session))
                    .andExpect(jsonPath("$.email").value(newEmail));
            // A recuperação de senha passa a usar o novo endereço.
            mockMvc.perform(post("/api/auth/forgot-password").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"%s\"}".formatted(newEmail)))
                    .andExpect(status().isAccepted());
            assertThat(emailGateway.awaitMessageTo(newEmail).body()).contains("#token=");
        }

        @Test
        void rejectsEmailOfAnotherCandidate() throws Exception {
            RegisteredCandidate first = auth.registerCandidate();
            RegisteredCandidate second = auth.registerCandidate();
            Cookie session = auth.login(first.cpf(), first.password());

            changeEmail(session, second.email(), first.password()).andExpect(status().isConflict());
        }

        private ResultActions changeEmail(Cookie session, String newEmail, String currentPassword) throws Exception {
            return mockMvc.perform(put("/api/candidate/me/email").with(csrf()).cookie(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"newEmail\": \"%s\", \"currentPassword\": \"%s\"}".formatted(newEmail, currentPassword)));
        }
    }

    @Nested
    class ChangePassword {

        @Test
        void changesPasswordKeepsCurrentSessionAndEndsOthers() throws Exception {
            RegisteredCandidate candidate = auth.registerCandidate();
            Cookie otherDevice = auth.login(candidate.cpf(), candidate.password());
            Cookie currentDevice = auth.login(candidate.cpf(), candidate.password());

            changePassword(currentDevice, candidate.password(), "Nova#Senha2025").andExpect(status().isNoContent());

            mockMvc.perform(get("/api/auth/session").cookie(currentDevice)).andExpect(status().isOk());
            mockMvc.perform(get("/api/auth/session").cookie(otherDevice)).andExpect(status().isUnauthorized());
            assertThat(auth.loginStatus(candidate.cpf(), candidate.password())).isEqualTo(401);
            assertThat(auth.loginStatus(candidate.cpf(), "Nova#Senha2025")).isEqualTo(200);
            assertThat(emailGateway.awaitMessageTo(candidate.email()).subject()).contains("Senha alterada");
        }

        @Test
        void appliesPasswordPolicy() throws Exception {
            RegisteredCandidate candidate = auth.registerCandidate();
            Cookie session = auth.login(candidate.cpf(), candidate.password());

            changePassword(session, candidate.password(), "Maria#2025x")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasItem("A senha não pode conter seu nome ou sobrenome.")));
        }

        @Test
        void repeatedWrongCurrentPasswordLocksAccount() throws Exception {
            RegisteredCandidate candidate = auth.registerCandidate();
            Cookie session = auth.login(candidate.cpf(), candidate.password());

            for (int attempt = 0; attempt < 5; attempt++) {
                changePassword(session, "Errada#2024x", "Nova#Senha2025").andExpect(status().isBadRequest());
            }

            assertThat(auth.loginStatus(candidate.cpf(), candidate.password())).isEqualTo(401);
        }

        private ResultActions changePassword(Cookie session, String currentPassword, String newPassword)
                throws Exception {
            return mockMvc.perform(put("/api/auth/password").with(csrf()).cookie(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"currentPassword\": \"%s\", \"newPassword\": \"%s\"}"
                            .formatted(currentPassword, newPassword)));
        }
    }
}
