package br.gov.pmps.processoseletivo.support;

import static br.gov.pmps.processoseletivo.support.TestData.loginJson;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.pmps.processoseletivo.PostgresIntegrationTest;
import br.gov.pmps.processoseletivo.security.mfa.MfaSecretCipher;
import br.gov.pmps.processoseletivo.security.mfa.TotpService;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cria administradores de teste com um papel contendo exatamente as permissões informadas e com
 * segundo fator já cadastrado; o login passa pela senha e pelo código TOTP, como em uso real.
 */
public final class AdminTestAccounts {

    public static final String PASSWORD = "Admin#Teste2025";

    public record Admin(UUID accountId, String cpf, String mfaSecret) {
    }

    private static final TotpService TOTP = new TotpService();
    private static final MfaSecretCipher CIPHER = new MfaSecretCipher(PostgresIntegrationTest.MFA_TEST_KEY);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final MockMvc mockMvc;

    public AdminTestAccounts(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, MockMvc mockMvc) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.mockMvc = mockMvc;
    }

    /** @return cookie de sessão de um administrador com as permissões informadas */
    public Cookie sessionWith(String... permissions) throws Exception {
        Admin admin = create(permissions);
        return login(admin);
    }

    public Admin create(String... permissions) {
        String cpf = TestData.randomCpf();
        UUID accountId = UUID.randomUUID();
        String secret = TOTP.newSecret();
        Timestamp now = Timestamp.from(Instant.now());
        String roleCode = "TEST_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();

        jdbcTemplate.update("""
                insert into user_account (id, account_type, cpf, email, password_hash, active, failed_login_attempts,
                                          password_changed_at, created_at, updated_at, version,
                                          mfa_secret_encrypted, mfa_enabled_at)
                values (?, 'ADMIN', ?, ?, ?, true, 0, ?, ?, ?, 0, ?, ?)
                """, accountId, cpf, "admin-" + UUID.randomUUID() + "@example.test",
                passwordEncoder.encode(PASSWORD), now, now, now, CIPHER.encrypt(secret), now);
        jdbcTemplate.update("""
                insert into administrator (id, user_account_id, full_name, created_at, updated_at, version)
                values (?, ?, 'Administrador de Teste', ?, ?, 0)
                """, UUID.randomUUID(), accountId, now, now);
        jdbcTemplate.update("insert into role (code, name) values (?, ?)", roleCode, "Papel de teste " + roleCode);
        for (String permission : permissions) {
            jdbcTemplate.update("insert into role_permission (role_code, permission_code) values (?, ?)",
                    roleCode, permission);
        }
        jdbcTemplate.update("insert into user_account_role (user_account_id, role_code) values (?, ?)",
                accountId, roleCode);
        return new Admin(accountId, cpf, secret);
    }

    public Cookie login(Admin admin) throws Exception {
        Cookie pending = mockMvc.perform(post("/api/admin/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(admin.cpf(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(AuthTestClient.SESSION_COOKIE);
        Cookie session = mockMvc.perform(post("/api/admin/auth/mfa/verify").with(csrf()).cookie(pending)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"%s\"}".formatted(currentCode(admin.mfaSecret()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(AuthTestClient.SESSION_COOKIE);
        if (session == null) {
            throw new AssertionError("Login administrativo não devolveu cookie de sessão");
        }
        return session;
    }

    // O servidor recusa reutilizar o passo de tempo; logins repetidos da mesma conta usam o passo seguinte.
    private static final Map<String, Long> LAST_STEP = new ConcurrentHashMap<>();

    /** Código ainda não usado para o segredo (aguarda o próximo passo de 30 s quando necessário). */
    public static synchronized String currentCode(String secret) {
        long current = Instant.now().getEpochSecond() / 30;
        long step = Math.max(current, LAST_STEP.getOrDefault(secret, Long.MIN_VALUE) + 1);
        while (step > current + 1) {
            sleepQuietly();
            current = Instant.now().getEpochSecond() / 30;
        }
        LAST_STEP.put(secret, step);
        return TOTP.codeAt(secret, Instant.ofEpochSecond(step * 30));
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
