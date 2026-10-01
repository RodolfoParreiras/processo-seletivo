package br.gov.pmps.processoseletivo.application.usecase;

import br.gov.pmps.processoseletivo.application.dto.ResetPasswordRequest;
import br.gov.pmps.processoseletivo.application.service.AccountSessionRegistry;
import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.application.service.PasswordResetTokenIssuer;
import br.gov.pmps.processoseletivo.domain.model.PasswordResetToken;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import br.gov.pmps.processoseletivo.domain.repository.AdministratorRepository;
import br.gov.pmps.processoseletivo.domain.repository.CandidateRepository;
import br.gov.pmps.processoseletivo.domain.repository.PasswordResetTokenRepository;
import br.gov.pmps.processoseletivo.domain.repository.UserAccountRepository;
import br.gov.pmps.processoseletivo.domain.rule.PasswordPolicy;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResetPasswordUseCase {

    static final String INVALID_LINK = "Link inválido ou expirado. Solicite um novo link de redefinição de senha.";

    private final PasswordResetTokenRepository tokenRepository;
    private final UserAccountRepository accountRepository;
    private final CandidateRepository candidateRepository;
    private final AdministratorRepository administratorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountSessionRegistry sessionRegistry;
    private final AuditService auditService;
    private final Clock clock;

    public ResetPasswordUseCase(
            PasswordResetTokenRepository tokenRepository,
            UserAccountRepository accountRepository,
            CandidateRepository candidateRepository,
            AdministratorRepository administratorRepository,
            PasswordEncoder passwordEncoder,
            AccountSessionRegistry sessionRegistry,
            AuditService auditService,
            Clock clock) {
        this.tokenRepository = tokenRepository;
        this.accountRepository = accountRepository;
        this.candidateRepository = candidateRepository;
        this.administratorRepository = administratorRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public void execute(ResetPasswordRequest request, String ipAddress) {
        Instant now = clock.instant();
        PasswordResetToken token = tokenRepository.findForUse(PasswordResetTokenIssuer.hash(request.token()))
                .filter(candidateToken -> candidateToken.isUsable(now))
                .orElseThrow(ResetPasswordUseCase::invalidLink);
        UserAccount account = accountRepository.findById(token.getUserAccountId())
                .filter(UserAccount::isActive)
                .orElseThrow(ResetPasswordUseCase::invalidLink);

        List<String> violations = policyViolations(account, request.newPassword());
        if (!violations.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "A senha não atende à política de segurança.", violations);
        }

        account.changePassword(passwordEncoder.encode(request.newPassword()), now);
        token.markUsed(now);
        tokenRepository.invalidateAll(account.getId(), now);
        sessionRegistry.terminateAllSessions(account.getId());

        auditService.record(new AuditService.Entry(
                "PASSWORD_CHANGED", AuditService.Outcome.SUCCESS, account.getId(), "USER_ACCOUNT",
                account.getId().toString(), ipAddress, Map.of("method", token.getPurpose().name())));
    }

    private List<String> policyViolations(UserAccount account, String newPassword) {
        String fullName;
        LocalDate birthDate = null;
        switch (account.getAccountType()) {
            case CANDIDATE -> {
                var candidate = candidateRepository.findByUserAccountId(account.getId()).orElseThrow();
                fullName = candidate.getFullName();
                birthDate = candidate.getBirthDate();
            }
            case ADMIN -> fullName = administratorRepository.findByUserAccountId(account.getId())
                    .orElseThrow()
                    .getFullName();
            default -> throw new IllegalStateException("Tipo de conta desconhecido");
        }
        return PasswordPolicy.violations(newPassword, fullName, birthDate);
    }

    private static BusinessException invalidLink() {
        return new BusinessException(HttpStatus.BAD_REQUEST, INVALID_LINK);
    }
}
