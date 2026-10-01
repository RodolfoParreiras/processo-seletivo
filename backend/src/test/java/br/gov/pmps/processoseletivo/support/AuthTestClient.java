package br.gov.pmps.processoseletivo.support;

import static br.gov.pmps.processoseletivo.support.TestData.loginJson;
import static br.gov.pmps.processoseletivo.support.TestData.registrationJson;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Operações de cadastro e login reutilizadas pelos testes de integração. */
public final class AuthTestClient {

    public static final String SESSION_COOKIE = "SESSION";

    public record RegisteredCandidate(String cpf, String email, String password) {
    }

    private final MockMvc mockMvc;

    public AuthTestClient(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public RegisteredCandidate registerCandidate() throws Exception {
        RegisteredCandidate candidate =
                new RegisteredCandidate(TestData.randomCpf(), TestData.randomEmail(), TestData.VALID_PASSWORD);
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(candidate.cpf(), candidate.email(), candidate.password())))
                .andExpect(status().isCreated());
        return candidate;
    }

    public Cookie login(String cpf, String password) throws Exception {
        Cookie session = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(cpf, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(SESSION_COOKIE);
        if (session == null) {
            throw new AssertionError("Login não devolveu cookie de sessão");
        }
        return session;
    }

    public int loginStatus(String cpf, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(cpf, password)))
                .andReturn().getResponse().getStatus();
    }
}
