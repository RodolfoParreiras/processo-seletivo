package br.gov.pmps.processoseletivo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.pmps.processoseletivo.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SecurityBaselineTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void anonymousRequestToUnmappedEndpointIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/processes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserIsStillDeniedOnEndpointsNotExplicitlyAllowed() throws Exception {
        mockMvc.perform(get("/api/admin/processes")
                        .with(user("qualquer").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void stateChangingRequestWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isForbidden());
    }

    @Test
    void stateChangingRequestWithCsrfTokenIsStillDeniedByDefault() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpointIsPublicAndDoesNotExposeDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void otherActuatorEndpointsAreNotAvailable() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    @Test
    void noDefaultInMemoryUserIsCreated() {
        assertThat(applicationContext.getBeanNamesForType(UserDetailsService.class)).isEmpty();
    }

    @Test
    void unauthorizedResponseDoesNotLeakInternalDetails() throws Exception {
        mockMvc.perform(get("/api/admin/processes"))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at br.gov"))));
    }
}
