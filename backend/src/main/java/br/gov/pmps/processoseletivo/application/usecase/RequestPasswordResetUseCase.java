package br.gov.pmps.processoseletivo.application.usecase;

import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.application.service.PasswordResetTokenIssuer;
import br.gov.pmps.processoseletivo.domain.model.AccountType;
import br.gov.pmps.processoseletivo.domain.model.PasswordResetToken;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import br.gov.pmps.processoseletivo.domain.repository.PasswordResetTokenRepository;
import br.gov.pmps.processoseletivo.domain.repository.UserAccountRepository;
import br.gov.pmps.processoseletivo.infrastructure.configuration.AppProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sempre termina sem erro para quem chama: a resposta não pode revelar se o e-mail está cadastrado
 * (ESPECIFICACAO §12).
 */
@Service
public class RequestPasswordResetUseCase {

    // Evita que o endpoint seja usado para inundar a caixa de e-mail de alguém.
    static final Duration MIN_INTERVAL_BETWEEN_LINKS = Duration.ofMinutes(2);

    private final UserAccountRepository accountRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetTokenIssuer tokenIssuer;
    private final AuditService auditService;
    private final AppProperties.Security securityProperties;
    private final Clock clock;

    public RequestPasswordResetUseCase(
            UserAccountRepository accountRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordResetTokenIssuer tokenIssuer,
            AuditService auditService,
            AppProperties appProperties,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.tokenIssuer = tokenIssuer;
        this.auditService = auditService;
        this.securityProperties = appProperties.security();
        this.clock = clock;
    }

    @Transactional
    public void execute(AccountType accountType, String email, String ipAddress) {
        Instant now = clock.instant();
        Optional<UserAccount> found =
                accountRepository.findActiveByEmail(accountType, UserAccount.normalizeEmail(email));

        if (found.isEmpty()) {
            record(AuditService.Outcome.FAILURE, null, accountType, "UNKNOWN_EMAIL", ipAddress);
            return;
        }

        UserAccount account = found.get();
        if (tokenRepository.existsByUserAccountIdAndCreatedAtAfter(
                account.getId(), now.minus(MIN_INTERVAL_BETWEEN_LINKS))) {
            record(AuditService.Outcome.FAILURE, account, accountType, "TOO_SOON", ipAddress);
            return;
        }

        tokenIssuer.issue(account, PasswordResetToken.Purpose.PASSWORD_RESET,
                securityProperties.passwordResetTokenValidity(), now);
        record(AuditService.Outcome.SUCCESS, account, accountType, null, ipAddress);
    }

    private void record(
            AuditService.Outcome outcome, UserAccount account, AccountType accountType, String reason, String ip) {
        Map<String, String> details = reason == null
                ? Map.of("accountType", accountType.name())
                : Map.of("accountType", accountType.name(), "reason", reason);
        auditService.record(new AuditService.Entry(
                "PASSWORD_RESET_REQUESTED", outcome,
                account == null ? null : account.getId(),
                "USER_ACCOUNT",
                account == null ? null : account.getId().toString(),
                ip, details));
    }
}
