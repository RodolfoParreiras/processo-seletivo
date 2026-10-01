package br.gov.pmps.processoseletivo.application.service;

import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import br.gov.pmps.processoseletivo.infrastructure.configuration.AppProperties;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Confirma a senha atual antes de alterações sensíveis. Erros contam para o bloqueio da conta,
 * para que uma sessão roubada não sirva para descobrir a senha por tentativa.
 * O chamador deve usar transação com {@code noRollbackFor = BusinessException.class}.
 */
@Service
public class CurrentPasswordVerifier {

    static final String WRONG_PASSWORD = "Senha atual incorreta, ou acesso temporariamente bloqueado.";

    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AppProperties.Security securityProperties;

    public CurrentPasswordVerifier(
            PasswordEncoder passwordEncoder, AuditService auditService, AppProperties appProperties) {
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.securityProperties = appProperties.security();
    }

    public void verify(UserAccount account, String currentPassword, String operation, String ipAddress, Instant now) {
        if (account.isLocked(now)) {
            fail(account, operation, "ACCOUNT_LOCKED", ipAddress);
        }
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            account.registerFailedLogin(
                    now, securityProperties.maxFailedLoginAttempts(), securityProperties.accountLockDuration());
            fail(account, operation, "INVALID_PASSWORD", ipAddress);
        }
    }

    private void fail(UserAccount account, String operation, String reason, String ipAddress) {
        auditService.record(new AuditService.Entry(
                operation, AuditService.Outcome.FAILURE, account.getId(), "USER_ACCOUNT",
                account.getId().toString(), ipAddress, Map.of("reason", reason)));
        throw new BusinessException(HttpStatus.BAD_REQUEST, WRONG_PASSWORD);
    }
}
