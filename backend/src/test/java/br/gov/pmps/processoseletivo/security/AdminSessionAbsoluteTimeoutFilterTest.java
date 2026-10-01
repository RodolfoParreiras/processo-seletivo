package br.gov.pmps.processoseletivo.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.gov.pmps.processoseletivo.domain.model.AccountType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AdminSessionAbsoluteTimeoutFilterTest {

    private static final Instant LOGIN_TIME = Instant.parse("2026-10-01T08:00:00Z");
    private static final Duration ABSOLUTE_TIMEOUT = Duration.ofHours(8);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void keepsAdminSessionWithinAbsoluteTimeout() throws Exception {
        MockHttpSession session = authenticate(AccountType.ADMIN);

        filterAt(LOGIN_TIME.plus(Duration.ofHours(7)), session);

        assertThat(session.isInvalid()).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void invalidatesAdminSessionAfterAbsoluteTimeout() throws Exception {
        MockHttpSession session = authenticate(AccountType.ADMIN);

        filterAt(LOGIN_TIME.plus(Duration.ofHours(8)).plusSeconds(1), session);

        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotApplyAbsoluteTimeoutToCandidates() throws Exception {
        MockHttpSession session = authenticate(AccountType.CANDIDATE);

        filterAt(LOGIN_TIME.plus(Duration.ofHours(9)), session);

        assertThat(session.isInvalid()).isFalse();
    }

    @Test
    void deniesAdminSessionWithoutLoginTime() throws Exception {
        MockHttpSession session = authenticate(AccountType.ADMIN);
        session.removeAttribute(SessionLoginService.AUTHENTICATED_AT_ATTRIBUTE);

        filterAt(LOGIN_TIME, session);

        assertThat(session.isInvalid()).isTrue();
    }

    private static MockHttpSession authenticate(AccountType accountType) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionLoginService.AUTHENTICATED_AT_ATTRIBUTE, LOGIN_TIME.toEpochMilli());
        AuthenticatedAccount principal = new AuthenticatedAccount(UUID.randomUUID(), accountType, "Teste");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
        return session;
    }

    private static void filterAt(Instant now, MockHttpSession session) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new AdminSessionAbsoluteTimeoutFilter(ABSOLUTE_TIMEOUT, Clock.fixed(now, ZoneOffset.UTC))
                .doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }
}
