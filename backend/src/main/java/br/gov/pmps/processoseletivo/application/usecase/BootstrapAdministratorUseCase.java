package br.gov.pmps.processoseletivo.application.usecase;

import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.application.service.PasswordResetTokenIssuer;
import br.gov.pmps.processoseletivo.domain.model.AccountType;
import br.gov.pmps.processoseletivo.domain.model.Administrator;
import br.gov.pmps.processoseletivo.domain.model.PasswordResetToken;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import br.gov.pmps.processoseletivo.domain.repository.AdministratorRepository;
import br.gov.pmps.processoseletivo.domain.repository.UserAccountRepository;
import br.gov.pmps.processoseletivo.domain.rule.Cpf;
import br.gov.pmps.processoseletivo.infrastructure.configuration.AppProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o primeiro Administrador Geral somente quando não existe nenhuma conta administrativa.
 * Não há senha inicial: o administrador define a própria senha pelo link enviado por e-mail
 * (AI_RULES §30: nenhum usuário ou senha embutidos no sistema).
 */
@Service
public class BootstrapAdministratorUseCase {

    static final String GENERAL_ADMINISTRATOR_ROLE = "ADMINISTRADOR_GERAL";

    private final UserAccountRepository accountRepository;
    private final AdministratorRepository administratorRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenIssuer tokenIssuer;
    private final AuditService auditService;
    private final AppProperties.Security securityProperties;
    private final Clock clock;

    public BootstrapAdministratorUseCase(
            UserAccountRepository accountRepository,
            AdministratorRepository administratorRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetTokenIssuer tokenIssuer,
            AuditService auditService,
            AppProperties appProperties,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.administratorRepository = administratorRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.auditService = auditService;
        this.securityProperties = appProperties.security();
        this.clock = clock;
    }

    /** @return {@code false} quando já existe administrador e nada foi feito */
    @Transactional
    public boolean execute(String cpf, String fullName, String email) {
        if (accountRepository.existsByAccountType(AccountType.ADMIN)) {
            return false;
        }
        Instant now = clock.instant();
        // Senha aleatória descartada: a conta fica inacessível até o administrador usar o link.
        String unusablePasswordHash = passwordEncoder.encode(UUID.randomUUID() + UUID.randomUUID().toString());
        UserAccount account = new UserAccount(AccountType.ADMIN, Cpf.of(cpf), email, unusablePasswordHash, now);
        accountRepository.saveAndFlush(account);
        Administrator administrator = administratorRepository.save(new Administrator(account.getId(), fullName, now));
        accountRepository.assignRole(account.getId(), GENERAL_ADMINISTRATOR_ROLE);

        tokenIssuer.issue(account, PasswordResetToken.Purpose.PASSWORD_SETUP,
                securityProperties.passwordSetupTokenValidity(), now);

        auditService.record(new AuditService.Entry(
                "ADMINISTRATOR_BOOTSTRAPPED", AuditService.Outcome.SUCCESS, null, "ADMINISTRATOR",
                administrator.getId().toString(), null, Map.of("role", GENERAL_ADMINISTRATOR_ROLE)));
        return true;
    }
}
