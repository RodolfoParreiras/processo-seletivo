package br.gov.pmps.processoseletivo.security;

import br.gov.pmps.processoseletivo.domain.model.AccountType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Encerra sessões administrativas após um prazo absoluto, mesmo com atividade contínua
 * (ESPECIFICACAO §42: sessões administrativas mais restritivas).
 */
public class AdminSessionAbsoluteTimeoutFilter extends OncePerRequestFilter {

    private final Duration absoluteTimeout;
    private final Clock clock;
    private final SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public AdminSessionAbsoluteTimeoutFilter(Duration absoluteTimeout, Clock clock) {
        this.absoluteTimeout = absoluteTimeout;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && isExpiredAdminSession(session)) {
            session.invalidate();
            contextHolderStrategy.clearContext();
        }
        chain.doFilter(request, response);
    }

    private boolean isExpiredAdminSession(HttpSession session) {
        Authentication authentication = contextHolderStrategy.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedAccount account)
                || account.accountType() != AccountType.ADMIN) {
            return false;
        }
        // Sem o horário do login não há como provar que a sessão está no prazo: nega (AI_RULES §74).
        if (!(session.getAttribute(SessionLoginService.AUTHENTICATED_AT_ATTRIBUTE) instanceof Long authenticatedAt)) {
            return true;
        }
        return clock.millis() - authenticatedAt > absoluteTimeout.toMillis();
    }
}
