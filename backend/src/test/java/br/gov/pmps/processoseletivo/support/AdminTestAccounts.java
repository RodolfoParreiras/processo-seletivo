package br.gov.pmps.processoseletivo.support;

import static br.gov.pmps.processoseletivo.support.TestData.loginJson;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cria administradores de teste com um papel contendo exatamente as permissões informadas.
 * A gestão de administradores pela interface só existe na fase 7.
 */
public final class AdminTestAccounts {

    public static final String PASSWORD = "Admin#Teste2025";

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
        String cpf = TestData.randomCpf();
        UUID accountId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        String roleCode = "TEST_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();

        jdbcTemplate.update("""
                insert into user_account (id, account_type, cpf, email, password_hash, active, failed_login_attempts,
                                          password_changed_at, created_at, updated_at, version)
                values (?, 'ADMIN', ?, ?, ?, true, 0, ?, ?, ?, 0)
                """, accountId, cpf, "admin-" + UUID.randomUUID() + "@example.test",
                passwordEncoder.encode(PASSWORD), now, now, now);
        jdbcTemplate.update("""
                insert into administrator (id, user_account_id, full_name, created_at, updated_at, version)
                values (?, ?, 'Administrador de Teste', ?, ?, 0)
                """, UUID.randomUUID(), accountId, now, now);
        jdbcTemplate.update("insert into role (code, name) values (?, ?)", roleCode, "Papel de teste");
        for (String permission : permissions) {
            jdbcTemplate.update("insert into role_permission (role_code, permission_code) values (?, ?)",
                    roleCode, permission);
        }
        jdbcTemplate.update("insert into user_account_role (user_account_id, role_code) values (?, ?)",
                accountId, roleCode);

        Cookie session = mockMvc.perform(post("/api/admin/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(cpf, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(AuthTestClient.SESSION_COOKIE);
        if (session == null) {
            throw new AssertionError("Login administrativo não devolveu cookie de sessão");
        }
        return session;
    }
}
