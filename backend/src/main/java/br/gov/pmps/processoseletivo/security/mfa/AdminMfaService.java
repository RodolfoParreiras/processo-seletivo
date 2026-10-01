package br.gov.pmps.processoseletivo.security.mfa;

import br.gov.pmps.processoseletivo.application.dto.AuthenticationResult;
import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import br.gov.pmps.processoseletivo.domain.repository.UserAccountRepository;
import br.gov.pmps.processoseletivo.domain.rule.Cpf;
import br.gov.pmps.processoseletivo.infrastructure.configuration.AppProperties;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Segundo fator obrigatório no login administrativo (ESPECIFICACAO §76).
 * Depois da senha, a sessão guarda apenas um estado "pendente" que não concede acesso; o acesso só
 * existe após o código TOTP correto. Códigos errados contam para o bloqueio da conta.
 */
@Service
public class AdminMfaService {

    static final String PENDING_ATTRIBUTE = "app.mfa.pending";
    static final Duration PENDING_VALIDITY = Duration.ofMinutes(5);
    static final String INVALID_CODE = "Código inválido ou expirado, ou acesso temporariamente bloqueado.";

    /** Estado entre a senha e o segundo fator. Não é autenticação. */
    record PendingMfa(
            UUID accountId, String displayName, boolean enrolled, Instant createdAt, String enrollmentSecret) implements Serializable {
    }

    public record SetupResponse(String secret, String provisioningUri) {
    }

    private final UserAccountRepository accountRepository;
    private final TotpService totpService;
    private final MfaSecretCipher cipher;
    private final AuditService auditService;
    private final AppProperties.Security securityProperties;
    private final Clock clock;

    public AdminMfaService(
            UserAccountRepository accountRepository,
            TotpService totpService,
            MfaSecretCipher cipher,
            AuditService auditService,
            AppProperties appProperties,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.totpService = totpService;
        this.cipher = cipher;
        this.auditService = auditService;
        this.securityProperties = appProperties.security();
        this.clock = clock;
    }

    /**
     * Senha aceita: inicia o desafio. Uma sessão anterior é descartada (proteção contra fixation).
     *
     * @return se a conta ainda precisa cadastrar o aplicativo autenticador
     */
    public boolean startChallenge(AuthenticationResult result, HttpServletRequest request) {
        boolean enrolled = result.mfaEnabled();
        HttpSession previous = request.getSession(false);
        if (previous != null) {
            previous.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval((int) PENDING_VALIDITY.toSeconds());
        session.setAttribute(PENDING_ATTRIBUTE, new PendingMfa(
                result.accountId(), result.displayName(), enrolled, clock.instant(), null));
        return !enrolled;
    }

    /** Cadastro do autenticador para quem ainda não tem MFA. O segredo só é gravado após o primeiro código válido. */
    @Transactional(readOnly = true)
    public SetupResponse setup(HttpServletRequest request) {
        PendingMfa pending = requirePending(request);
        if (pending.enrolled()) {
            throw new BusinessException(HttpStatus.CONFLICT, "O segundo fator já está configurado para esta conta.");
        }
        String secret = totpService.newSecret();
        request.getSession(false).setAttribute(PENDING_ATTRIBUTE, new PendingMfa(
                pending.accountId(), pending.displayName(), false, pending.createdAt(), secret));
        UserAccount account = accountRepository.findById(pending.accountId()).orElseThrow();
        return new SetupResponse(secret, totpService.provisioningUri(secret, maskedCpfLabel(account)));
    }

    /**
     * Confere o código. Em caso de sucesso devolve o resultado da autenticação para abrir a sessão.
     * Erros são gravados mesmo com a exceção, para que o bloqueio funcione.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthenticationResult verify(String code, HttpServletRequest request) {
        PendingMfa pending = requirePending(request);
        Instant now = clock.instant();
        String ip = request.getRemoteAddr();
        UserAccount account = accountRepository.findById(pending.accountId()).orElseThrow();

        if (!account.isActive() || account.isLocked(now)) {
            fail(account, "ACCOUNT_LOCKED_OR_INACTIVE", ip);
        }
        String secret = pending.enrolled() ? cipher.decrypt(account.getMfaSecretEncrypted()) : pending.enrollmentSecret();
        if (secret == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "Configure o aplicativo autenticador antes de informar o código.");
        }
        OptionalLong step = totpService.verify(secret, code, now, account.getMfaLastUsedStep());
        if (step.isEmpty()) {
            account.registerFailedLogin(
                    now, securityProperties.maxFailedLoginAttempts(), securityProperties.accountLockDuration());
            fail(account, "INVALID_MFA_CODE", ip);
        }

        if (!pending.enrolled()) {
            account.enableMfa(cipher.encrypt(secret), now);
            audit("MFA_ENROLLED", account, ip, Map.of());
        }
        account.registerMfaUse(step.getAsLong());
        account.registerSuccessfulLogin(now);
        audit("LOGIN", account, ip, Map.of("accountType", "ADMIN", "mfa", true));
        return new AuthenticationResult(
                account.getId(), account.getAccountType(), pending.displayName(),
                Set.copyOf(accountRepository.findPermissionCodes(account.getId())));
    }

    private PendingMfa requirePending(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object attribute = session == null ? null : session.getAttribute(PENDING_ATTRIBUTE);
        if (!(attribute instanceof PendingMfa pending)
                || pending.createdAt().plus(PENDING_VALIDITY).isBefore(clock.instant())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Sessão de login expirada. Entre novamente.");
        }
        return pending;
    }

    private void fail(UserAccount account, String reason, String ip) {
        audit("LOGIN", account, ip, Map.of("accountType", "ADMIN", "reason", reason), AuditService.Outcome.FAILURE);
        throw new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_CODE);
    }

    private void audit(String action, UserAccount account, String ip, Map<String, ?> details) {
        audit(action, account, ip, details, AuditService.Outcome.SUCCESS);
    }

    private void audit(String action, UserAccount account, String ip, Map<String, ?> details, AuditService.Outcome outcome) {
        auditService.record(new AuditService.Entry(action, outcome, account.getId(), "USER_ACCOUNT",
                account.getId().toString(), ip, details));
    }

    private static String maskedCpfLabel(UserAccount account) {
        return "CPF " + Cpf.mask(account.getCpf());
    }
}
