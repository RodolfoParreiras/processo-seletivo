package br.gov.pmps.processoseletivo.application.usecase;

import br.gov.pmps.processoseletivo.application.dto.ChangePasswordRequest;
import br.gov.pmps.processoseletivo.application.service.AccountPasswordPolicy;
import br.gov.pmps.processoseletivo.application.service.AccountSecurityNotice;
import br.gov.pmps.processoseletivo.application.service.AccountSessionRegistry;
import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.application.service.CurrentPasswordVerifier;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import br.gov.pmps.processoseletivo.domain.repository.PasswordResetTokenRepository;
import br.gov.pmps.processoseletivo.domain.repository.UserAccountRepository;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Troca de senha com a sessão ativa, para candidatos e administradores. */
@Service
public class ChangePasswordUseCase {

    private final UserAccountRepository accountRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final CurrentPasswordVerifier currentPasswordVerifier;
    private final AccountPasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final AccountSessionRegistry sessionRegistry;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ChangePasswordUseCase(
            UserAccountRepository accountRepository,
            PasswordResetTokenRepository tokenRepository,
            CurrentPasswordVerifier currentPasswordVerifier,
            AccountPasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder,
            AccountSessionRegistry sessionRegistry,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.currentPasswordVerifier = currentPasswordVerifier;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    // Senha atual errada precisa ser gravada para o bloqueio; nenhuma alteração ocorre antes das verificações.
    @Transactional(noRollbackFor = BusinessException.class)
    public void execute(UUID accountId, ChangePasswordRequest request, String currentSessionId, String ipAddress) {
        Instant now = clock.instant();
        UserAccount account = accountRepository.findById(accountId).orElseThrow();

        currentPasswordVerifier.verify(account, request.currentPassword(), "PASSWORD_CHANGED", ipAddress, now);
        passwordPolicy.validate(account, request.newPassword());

        account.changePassword(passwordEncoder.encode(request.newPassword()), now);
        // Links de redefinição pendentes deixam de valer.
        tokenRepository.invalidateAll(account.getId(), now);
        sessionRegistry.terminateOtherSessions(account.getId(), currentSessionId);
        eventPublisher.publishEvent(
                new AccountSecurityNotice(account.getEmail(), AccountSecurityNotice.Type.PASSWORD_CHANGED));

        auditService.record(new AuditService.Entry(
                "PASSWORD_CHANGED", AuditService.Outcome.SUCCESS, account.getId(), "USER_ACCOUNT",
                account.getId().toString(), ipAddress, Map.of("method", "AUTHENTICATED_CHANGE")));
    }
}
