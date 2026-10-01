package br.gov.pmps.processoseletivo.presentation;

import static br.gov.pmps.processoseletivo.support.TestData.VALID_PASSWORD;
import static br.gov.pmps.processoseletivo.support.TestData.loginJson;
import static br.gov.pmps.processoseletivo.support.TestData.randomCpf;
import static br.gov.pmps.processoseletivo.support.TestData.randomEmail;
import static br.gov.pmps.processoseletivo.support.TestData.registrationJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.pmps.processoseletivo.PostgresIntegrationTest;
import br.gov.pmps.processoseletivo.application.usecase.BootstrapAdministratorUseCase;
import br.gov.pmps.processoseletivo.support.AdminTestAccounts;
import br.gov.pmps.processoseletivo.support.RecordingEmailGateway;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@AutoConfigureMockMvc
// Primeira classe a rodar: o teste de provisionamento exige banco sem administradores (junit-platform.properties).
@Order(1)
class AuthenticationFlowTest extends PostgresIntegrationTest {

    private static final String SESSION_COOKIE = "SESSION";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingEmailGateway emailGateway;

    @Autowired
    private BootstrapAdministratorUseCase bootstrapAdministrator;

    @Nested
    class Registration {

        @Test
        void registersCandidateWithHashedPasswordAndAudit() throws Exception {
            String cpf = randomCpf();

            register(cpf, randomEmail(), VALID_PASSWORD).andExpect(status().isCreated());

            String passwordHash = jdbcTemplate.queryForObject(
                    "select password_hash from user_account where cpf = ? and account_type = 'CANDIDATE'",
                    String.class, cpf);
            assertThat(passwordHash).startsWith("$argon2id$").doesNotContain(VALID_PASSWORD);
            Integer audits = jdbcTemplate.queryForObject("""
                    select count(*) from audit_log l join user_account a on a.id = l.actor_user_account_id
                     where a.cpf = ? and l.action = 'CANDIDATE_REGISTERED'
                    """, Integer.class, cpf);
            assertThat(audits).isEqualTo(1);
        }

        @Test
        void rejectsDuplicateCpfAndEmailWithSameGenericMessage() throws Exception {
            String cpf = randomCpf();
            String email = randomEmail();
            register(cpf, email, VALID_PASSWORD).andExpect(status().isCreated());

            String duplicateCpf = register(cpf, randomEmail(), VALID_PASSWORD)
                    .andExpect(status().isConflict()).andReturn().getResponse().getContentAsString();
            String duplicateEmail = register(randomCpf(), email.toUpperCase(), VALID_PASSWORD)
                    .andExpect(status().isConflict()).andReturn().getResponse().getContentAsString();

            assertThat(detail(duplicateCpf)).isEqualTo(detail(duplicateEmail));
        }

        @Test
        void rejectsWeakPasswordListingViolations() throws Exception {
            register(randomCpf(), randomEmail(), "maria123")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasItem("A senha deve conter letra maiúscula.")))
                    .andExpect(jsonPath("$.violations", hasItem("A senha não pode conter seu nome ou sobrenome.")));
        }

        @Test
        void rejectsInvalidCpfWithoutEchoingValue() throws Exception {
            mockMvc.perform(post("/api/auth/register").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationJson("12345678900", randomEmail(), VALID_PASSWORD)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.cpf").value("CPF inválido."))
                    .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                            .doesNotContain("12345678900"));
        }

        @Test
        void personWithDisabilityMustInformAdaptations() throws Exception {
            String body = registrationJson(randomCpf(), randomEmail(), VALID_PASSWORD)
                    .replace("\"hasDisability\": false", "\"hasDisability\": true");

            mockMvc.perform(post("/api/auth/register").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post("/api/auth/register").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body.replace("\"adaptations\": []", "\"adaptations\": [\"LIBRAS_INTERPRETER\"]")))
                    .andExpect(status().isCreated());
        }

        @Test
        void requiresCsrfToken() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationJson(randomCpf(), randomEmail(), VALID_PASSWORD)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void concurrentRegistrationsWithSameCpfCreateOnlyOneAccount() throws Exception {
            String cpf = randomCpf();
            int attempts = 6;
            CountDownLatch start = new CountDownLatch(1);
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                tasks.add(() -> {
                    start.await();
                    return register(cpf, randomEmail(), VALID_PASSWORD).andReturn().getResponse().getStatus();
                });
            }
            List<Integer> statuses = new ArrayList<>();
            try (ExecutorService executor = Executors.newFixedThreadPool(attempts)) {
                List<Future<Integer>> futures = tasks.stream().map(executor::submit).toList();
                start.countDown();
                for (Future<Integer> future : futures) {
                    statuses.add(future.get());
                }
            }

            assertThat(statuses).filteredOn(statusCode -> statusCode == 201).hasSize(1);
            assertThat(statuses).filteredOn(statusCode -> statusCode != 201).containsOnly(409);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from user_account where cpf = ? and account_type = 'CANDIDATE'",
                    Integer.class, cpf)).isEqualTo(1);
        }
    }

    @Nested
    class Login {

        @Test
        void candidateLogsInAndReadsOwnSession() throws Exception {
            String cpf = registeredCandidate();

            Cookie session = login("/api/auth/login", cpf, VALID_PASSWORD);

            mockMvc.perform(get("/api/auth/session").cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountType").value("CANDIDATE"))
                    .andExpect(jsonPath("$.displayName").value("Maria Teste Silva"))
                    .andExpect(jsonPath("$.permissions").isEmpty());
        }

        @Test
        void sessionCookieIsHardened() throws Exception {
            String cpf = registeredCandidate();

            loginRequest("/api/auth/login", cpf, VALID_PASSWORD)
                    .andExpect(status().isOk())
                    .andExpect(cookie().httpOnly(SESSION_COOKIE, true))
                    .andExpect(cookie().secure(SESSION_COOKIE, true))
                    .andExpect(cookie().sameSite(SESSION_COOKIE, "Strict"));
        }

        @Test
        void wrongPasswordAndUnknownCpfGetIdenticalResponses() throws Exception {
            String cpf = registeredCandidate();

            String wrongPassword = loginRequest("/api/auth/login", cpf, "Errada#2024x")
                    .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
            String unknownCpf = loginRequest("/api/auth/login", randomCpf(), "Errada#2024x")
                    .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

            assertThat(wrongPassword).isEqualTo(unknownCpf);
        }

        @Test
        void accountIsLockedAfterFiveFailures() throws Exception {
            String cpf = registeredCandidate();
            for (int attempt = 0; attempt < 5; attempt++) {
                loginRequest("/api/auth/login", cpf, "Errada#2024x").andExpect(status().isUnauthorized());
            }

            loginRequest("/api/auth/login", cpf, VALID_PASSWORD).andExpect(status().isUnauthorized());
        }

        @Test
        void newLoginReplacesPreviousSession() throws Exception {
            String cpf = registeredCandidate();
            Cookie firstSession = login("/api/auth/login", cpf, VALID_PASSWORD);

            MvcResult secondLogin = mockMvc.perform(post("/api/auth/login").with(csrf()).cookie(firstSession)
                            .contentType(MediaType.APPLICATION_JSON).content(loginJson(cpf, VALID_PASSWORD)))
                    .andExpect(status().isOk()).andReturn();
            Cookie secondSession = secondLogin.getResponse().getCookie(SESSION_COOKIE);

            assertThat(secondSession).isNotNull();
            assertThat(secondSession.getValue()).isNotEqualTo(firstSession.getValue());
            mockMvc.perform(get("/api/auth/session").cookie(firstSession)).andExpect(status().isUnauthorized());
        }

        @Test
        void candidateCredentialsDoNotWorkOnAdministrativeLogin() throws Exception {
            String cpf = registeredCandidate();

            loginRequest("/api/admin/auth/login", cpf, VALID_PASSWORD).andExpect(status().isUnauthorized());
        }

        @Test
        void logoutEndsSession() throws Exception {
            String cpf = registeredCandidate();
            Cookie session = login("/api/auth/login", cpf, VALID_PASSWORD);

            mockMvc.perform(post("/api/auth/logout").with(csrf()).cookie(session))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/auth/session").cookie(session)).andExpect(status().isUnauthorized());
        }

        @Test
        void sessionEndpointRequiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/auth/session")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class PasswordRecovery {

        @Test
        void unknownAndKnownEmailsGetSameResponse() throws Exception {
            String email = randomEmail();
            register(randomCpf(), email, VALID_PASSWORD).andExpect(status().isCreated());

            String known = forgotPassword(email).andExpect(status().isAccepted())
                    .andReturn().getResponse().getContentAsString();
            String unknown = forgotPassword(randomEmail()).andExpect(status().isAccepted())
                    .andReturn().getResponse().getContentAsString();

            assertThat(known).isEqualTo(unknown);
        }

        @Test
        void resetChangesPasswordInvalidatesTokenAndEndsSessions() throws Exception {
            String cpf = randomCpf();
            String email = randomEmail();
            register(cpf, email, VALID_PASSWORD).andExpect(status().isCreated());
            Cookie existingSession = login("/api/auth/login", cpf, VALID_PASSWORD);

            forgotPassword(email).andExpect(status().isAccepted());
            String token = RecordingEmailGateway.extractToken(emailGateway.awaitMessageTo(email));

            resetPassword(token, "Nova#Senha2025").andExpect(status().isNoContent());

            resetPassword(token, "Outra#Senha2025").andExpect(status().isBadRequest());
            mockMvc.perform(get("/api/auth/session").cookie(existingSession)).andExpect(status().isUnauthorized());
            loginRequest("/api/auth/login", cpf, VALID_PASSWORD).andExpect(status().isUnauthorized());
            loginRequest("/api/auth/login", cpf, "Nova#Senha2025").andExpect(status().isOk());
        }

        @Test
        void tokenIsStoredOnlyAsHash() throws Exception {
            String email = randomEmail();
            register(randomCpf(), email, VALID_PASSWORD).andExpect(status().isCreated());
            forgotPassword(email).andExpect(status().isAccepted());
            String token = RecordingEmailGateway.extractToken(emailGateway.awaitMessageTo(email));

            Integer plainTokenRows = jdbcTemplate.queryForObject(
                    "select count(*) from password_reset_token where token_hash = ?", Integer.class, token);
            assertThat(plainTokenRows).isZero();
        }

        @Test
        void expiredTokenIsRejected() throws Exception {
            String email = randomEmail();
            register(randomCpf(), email, VALID_PASSWORD).andExpect(status().isCreated());
            forgotPassword(email).andExpect(status().isAccepted());
            String token = RecordingEmailGateway.extractToken(emailGateway.awaitMessageTo(email));
            jdbcTemplate.update("""
                    update password_reset_token set expires_at = now() - interval '1 minute'
                     where user_account_id = (select id from user_account where email = ?)
                    """, email);

            resetPassword(token, "Nova#Senha2025").andExpect(status().isBadRequest());
        }

        @Test
        void weakNewPasswordKeepsTokenUsable() throws Exception {
            String email = randomEmail();
            register(randomCpf(), email, VALID_PASSWORD).andExpect(status().isCreated());
            forgotPassword(email).andExpect(status().isAccepted());
            String token = RecordingEmailGateway.extractToken(emailGateway.awaitMessageTo(email));

            resetPassword(token, "Maria#15031990").andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasItem("A senha não pode conter seu nome ou sobrenome.")));
            resetPassword(token, "Nova#Senha2025").andExpect(status().isNoContent());
        }
    }

    @Nested
    class Administration {

        @Test
        void bootstrappedAdministratorDefinesPasswordByEmailAndLogsIn() throws Exception {
            String cpf = randomCpf();
            String email = "admin-" + randomEmail();
            // Único teste que cria administrador: o banco de teste começa sem nenhuma conta administrativa.
            assertThat(bootstrapAdministrator.execute(cpf, "Administrador Teste", email)).isTrue();
            String token = RecordingEmailGateway.extractToken(emailGateway.awaitMessageTo(email));

            resetPassword(token, "Admin#Forte2025").andExpect(status().isNoContent());

            // Senha correta só abre o desafio do segundo fator; ainda não há acesso.
            MvcResult passwordStep = loginRequest("/api/admin/auth/login", cpf, "Admin#Forte2025")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mfaRequired").value(true))
                    .andExpect(jsonPath("$.enrollmentRequired").value(true))
                    .andReturn();
            Cookie pending = passwordStep.getResponse().getCookie(SESSION_COOKIE);
            mockMvc.perform(get("/api/auth/session").cookie(pending)).andExpect(status().isUnauthorized());

            String setupBody = mockMvc.perform(post("/api/admin/auth/mfa/setup").with(csrf()).cookie(pending))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.provisioningUri", org.hamcrest.Matchers.startsWith("otpauth://totp/")))
                    .andReturn().getResponse().getContentAsString();
            String secret = setupBody.replaceAll(".*\"secret\":\"([A-Z2-7]+)\".*", "$1");

            mockMvc.perform(post("/api/admin/auth/mfa/verify").with(csrf()).cookie(pending)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"code\": \"000000\"}"))
                    .andExpect(status().isUnauthorized());
            Cookie session = mockMvc.perform(post("/api/admin/auth/mfa/verify").with(csrf()).cookie(pending)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\": \"%s\"}".formatted(AdminTestAccounts.currentCode(secret))))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getCookie(SESSION_COOKIE);
            assertThat(session.getValue()).isNotEqualTo(pending.getValue());
            assertThat(jdbcTemplate.queryForObject(
                    "select mfa_secret_encrypted from user_account where cpf = ? and account_type = 'ADMIN'",
                    String.class, cpf)).isNotNull().doesNotContain(secret);

            mockMvc.perform(get("/api/auth/session").cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountType").value("ADMIN"))
                    .andExpect(jsonPath("$.permissions", hasItem("AUDITORIA_VISUALIZAR")));
            // Conta administrativa não acessa a área do candidato com as mesmas credenciais.
            loginRequest("/api/auth/login", cpf, "Admin#Forte2025").andExpect(status().isUnauthorized());
            // O provisionamento só ocorre uma vez.
            assertThat(bootstrapAdministrator.execute(randomCpf(), "Outro", randomEmail())).isFalse();
        }
    }

    private ResultActions register(String cpf, String email, String password)
            throws Exception {
        return mockMvc.perform(post("/api/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationJson(cpf, email, password)));
    }

    private String registeredCandidate() throws Exception {
        String cpf = randomCpf();
        register(cpf, randomEmail(), VALID_PASSWORD).andExpect(status().isCreated());
        return cpf;
    }

    private ResultActions loginRequest(String path, String cpf, String password)
            throws Exception {
        return mockMvc.perform(post(path).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(cpf, password)));
    }

    private Cookie login(String path, String cpf, String password) throws Exception {
        Cookie session = loginRequest(path, cpf, password)
                .andExpect(status().isOk()).andReturn().getResponse().getCookie(SESSION_COOKIE);
        assertThat(session).isNotNull();
        return session;
    }

    private ResultActions forgotPassword(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/forgot-password").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\"}".formatted(email)));
    }

    private ResultActions resetPassword(String token, String newPassword)
            throws Exception {
        return mockMvc.perform(post("/api/auth/reset-password").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"%s\", \"newPassword\": \"%s\"}".formatted(token, newPassword)));
    }

    private static String detail(String problemJson) {
        int start = problemJson.indexOf("\"detail\":\"") + 10;
        return problemJson.substring(start, problemJson.indexOf('"', start));
    }
}
