package br.gov.pmps.processoseletivo.application.usecase;

import br.gov.pmps.processoseletivo.application.dto.ChangeEmailRequest;
import br.gov.pmps.processoseletivo.application.service.AccountSecurityNotice;
import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.application.service.CurrentPasswordVerifier;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import br.gov.pmps.processoseletivo.domain.repository.UserAccountRepository;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * O e-mail é o canal de recuperação de senha: trocá-lo exige a senha atual e
 * o endereço anterior é avisado da alteração.
 */
@Service
public class ChangeEmailUseCase {

    static final String EMAIL_UNAVAILABLE = "Este e-mail não pode ser utilizado. Informe outro endereço.";

    private final UserAccountRepository accountRepository;
    private final CurrentPasswordVerifier currentPasswordVerifier;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ChangeEmailUseCase(
            UserAccountRepository accountRepository,
            CurrentPasswordVerifier currentPasswordVerifier,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.currentPasswordVerifier = currentPasswordVerifier;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void execute(UUID accountId, ChangeEmailRequest request, String ipAddress) {
        Instant now = clock.instant();
        UserAccount account = accountRepository.findById(accountId).orElseThrow();
        String newEmail = UserAccount.normalizeEmail(request.newEmail());

        currentPasswordVerifier.verify(account, request.currentPassword(), "EMAIL_CHANGED", ipAddress, now);
        if (newEmail.equals(account.getEmail())) {
            return;
        }
        if (accountRepository.existsByAccountTypeAndEmail(account.getAccountType(), newEmail)) {
            throw new BusinessException(HttpStatus.CONFLICT, EMAIL_UNAVAILABLE);
        }

        String previousEmail = account.getEmail();
        // A unique constraint do banco cobre alterações simultâneas para o mesmo e-mail.
        account.changeEmail(newEmail, now);
        eventPublisher.publishEvent(new AccountSecurityNotice(previousEmail, AccountSecurityNotice.Type.EMAIL_CHANGED));

        auditService.record(new AuditService.Entry(
                "EMAIL_CHANGED", AuditService.Outcome.SUCCESS, account.getId(), "USER_ACCOUNT",
                account.getId().toString(), ipAddress, Map.of()));
    }
}
