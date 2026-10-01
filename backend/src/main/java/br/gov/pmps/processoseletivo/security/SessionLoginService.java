package br.gov.pmps.processoseletivo.security;

import br.gov.pmps.processoseletivo.application.dto.AuthenticationResult;
import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.domain.model.AccountType;
import br.gov.pmps.processoseletivo.infrastructure.configuration.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

/** Cria e encerra a sessão autenticada (ESPECIFICACAO §42). */
@Component
public class SessionLoginService {

    static final String AUTHENTICATED_AT_ATTRIBUTE = "app.authenticatedAt";

    private final SecurityContextRepository securityContextRepository;
    private final CsrfAuthenticationStrategy csrfAuthenticationStrategy;
    private final AuditService auditService;
    private final AppProperties.Security securityProperties;
    private final Clock clock;
    private final SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public SessionLoginService(
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            AuditService auditService,
            AppProperties appProperties,
            Clock clock) {
        this.securityContextRepository = securityContextRepository;
        this.csrfAuthenticationStrategy = new CsrfAuthenticationStrategy(csrfTokenRepository);
        this.auditService = auditService;
        this.securityProperties = appProperties.security();
        this.clock = clock;
    }

    public void establish(AuthenticationResult result, HttpServletRequest request, HttpServletResponse response) {
        // Descarta qualquer sessão anterior: impede session fixation e evita misturar contas no mesmo navegador.
        HttpSession previousSession = request.getSession(false);
        if (previousSession != null) {
            previousSession.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval((int) idleTimeout(result.accountType()).toSeconds());
        session.setAttribute(AUTHENTICATED_AT_ATTRIBUTE, clock.millis());

        AuthenticatedAccount principal =
                new AuthenticatedAccount(result.accountId(), result.accountType(), result.displayName());
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities(result));

        SecurityContext context = contextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        // Novo token CSRF após o login.
        csrfAuthenticationStrategy.onAuthentication(authentication, request, response);
    }

    /** Mantém o nome exibido em sincronia após o titular alterar o cadastro. */
    public void updateDisplayName(String displayName, HttpServletRequest request, HttpServletResponse response) {
        Authentication current = contextHolderStrategy.getContext().getAuthentication();
        if (current == null || !(current.getPrincipal() instanceof AuthenticatedAccount account)) {
            return;
        }
        AuthenticatedAccount updatedPrincipal =
                new AuthenticatedAccount(account.accountId(), account.accountType(), displayName);
        SecurityContext context = contextHolderStrategy.createEmptyContext();
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(updatedPrincipal, null, current.getAuthorities()));
        contextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    public void terminate(AuthenticatedAccount account, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        contextHolderStrategy.clearContext();
        csrfAuthenticationStrategy.onAuthentication(null, request, response);
        auditService.record(new AuditService.Entry(
                "LOGOUT", AuditService.Outcome.SUCCESS, account.accountId(), "USER_ACCOUNT",
                account.accountId().toString(), request.getRemoteAddr(), Map.of()));
    }

    private Duration idleTimeout(AccountType accountType) {
        return accountType == AccountType.ADMIN
                ? securityProperties.adminSessionIdleTimeout()
                : securityProperties.candidateSessionIdleTimeout();
    }

    private static List<GrantedAuthority> authorities(AuthenticationResult result) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + result.accountType().name()));
        result.permissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return authorities;
    }
}
