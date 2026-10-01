package br.gov.pmps.processoseletivo.application.usecase;

import br.gov.pmps.processoseletivo.application.dto.AuthenticationResult;
import br.gov.pmps.processoseletivo.application.dto.LoginRequest;
import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.domain.model.AccountType;
import br.gov.pmps.processoseletivo.domain.model.Administrator;
import br.gov.pmps.processoseletivo.domain.model.Candidate;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import br.gov.pmps.processoseletivo.domain.repository.AdministratorRepository;
import br.gov.pmps.processoseletivo.domain.repository.CandidateRepository;
import br.gov.pmps.processoseletivo.domain.repository.UserAccountRepository;
import br.gov.pmps.processoseletivo.domain.rule.Cpf;
import br.gov.pmps.processoseletivo.infrastructure.configuration.AppProperties;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica credenciais com resposta genérica (ESPECIFICACAO §44, AI_RULES §27/§55)
 * e bloqueio temporário após falhas consecutivas.
 */
@Service
public class LoginUseCase {

    static final String INVALID_CREDENTIALS =
            "CPF ou senha inválidos, ou acesso temporariamente bloqueado. Tente novamente mais tarde.";

    private final UserAccountRepository accountRepository;
    private final CandidateRepository candidateRepository;
    private final AdministratorRepository administratorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AppProperties.Security securityProperties;
    private final Clock clock;
    // Usado quando a conta não existe, para que o tempo de resposta não revele contas cadastradas.
    private final String dummyPasswordHash;

    public LoginUseCase(
            UserAccountRepository accountRepository,
            CandidateRepository candidateRepository,
            AdministratorRepository administratorRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            AppProperties appProperties,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.candidateRepository = candidateRepository;
        this.administratorRepository = administratorRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.securityProperties = appProperties.security();
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    // As falhas precisam ser gravadas mesmo com a exceção, para que o bloqueio funcione.
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthenticationResult execute(AccountType accountType, LoginRequest request, String ipAddress) {
        Instant now = clock.instant();
        String cpfDigits = request.cpf().replaceAll("\\D", "");
        Optional<UserAccount> found = Cpf.isValid(cpfDigits)
                ? accountRepository.findForLogin(accountType, cpfDigits)
                : Optional.empty();

        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            recordFailure(null, accountType, "UNKNOWN_ACCOUNT", ipAddress, Map.of("cpf", Cpf.mask(cpfDigits)));
            throw invalidCredentials();
        }

        UserAccount account = found.get();
        if (account.isLocked(now)) {
            recordFailure(account.getId(), accountType, "ACCOUNT_LOCKED", ipAddress, Map.of());
            throw invalidCredentials();
        }

        boolean passwordMatches = passwordEncoder.matches(request.password(), account.getPasswordHash());
        if (!passwordMatches || !account.isActive()) {
            if (!passwordMatches) {
                account.registerFailedLogin(
                        now, securityProperties.maxFailedLoginAttempts(), securityProperties.accountLockDuration());
            }
            String reason = account.isActive() ? "INVALID_PASSWORD" : "INACTIVE_ACCOUNT";
            recordFailure(account.getId(), accountType, reason, ipAddress,
                    Map.of("lockedNow", account.isLocked(now)));
            throw invalidCredentials();
        }

        account.registerSuccessfulLogin(now);
        auditService.record(new AuditService.Entry(
                "LOGIN", AuditService.Outcome.SUCCESS, account.getId(), "USER_ACCOUNT",
                account.getId().toString(), ipAddress, Map.of("accountType", accountType.name())));

        return new AuthenticationResult(account.getId(), accountType, displayName(account), permissions(account));
    }

    private String displayName(UserAccount account) {
        return switch (account.getAccountType()) {
            case CANDIDATE -> candidateRepository.findByUserAccountId(account.getId())
                    .map(Candidate::getFullName)
                    .orElseThrow(() -> new IllegalStateException("Conta de candidato sem cadastro"));
            case ADMIN -> administratorRepository.findByUserAccountId(account.getId())
                    .map(Administrator::getFullName)
                    .orElseThrow(() -> new IllegalStateException("Conta administrativa sem cadastro"));
        };
    }

    private Set<String> permissions(UserAccount account) {
        if (account.getAccountType() != AccountType.ADMIN) {
            return Set.of();
        }
        return Set.copyOf(accountRepository.findPermissionCodes(account.getId()));
    }

    private void recordFailure(
            UUID accountId, AccountType accountType, String reason, String ipAddress, Map<String, ?> extra) {
        Map<String, Object> details = new HashMap<>(extra);
        details.put("accountType", accountType.name());
        details.put("reason", reason);
        auditService.record(new AuditService.Entry(
                "LOGIN", AuditService.Outcome.FAILURE, accountId, "USER_ACCOUNT",
                accountId == null ? null : accountId.toString(), ipAddress, details));
    }

    private static BusinessException invalidCredentials() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
    }
}
