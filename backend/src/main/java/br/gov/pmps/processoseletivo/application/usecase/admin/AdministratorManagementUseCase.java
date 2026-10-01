package br.gov.pmps.processoseletivo.application.usecase.admin;

import br.gov.pmps.processoseletivo.application.service.AccountSessionRegistry;
import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.application.service.PasswordResetTokenIssuer;
import br.gov.pmps.processoseletivo.domain.model.AccountType;
import br.gov.pmps.processoseletivo.domain.model.Administrator;
import br.gov.pmps.processoseletivo.domain.model.PasswordResetToken;
import br.gov.pmps.processoseletivo.domain.model.Role;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import br.gov.pmps.processoseletivo.domain.repository.AdministratorRepository;
import br.gov.pmps.processoseletivo.domain.repository.PasswordResetTokenRepository;
import br.gov.pmps.processoseletivo.domain.repository.RoleRepository;
import br.gov.pmps.processoseletivo.domain.repository.UserAccountRepository;
import br.gov.pmps.processoseletivo.domain.rule.Cpf;
import br.gov.pmps.processoseletivo.infrastructure.configuration.AppProperties;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import br.gov.pmps.processoseletivo.shared.error.DomainRuleException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de administradores pelo Administrador Geral (ESPECIFICACAO §5.3). Nenhuma senha é definida
 * aqui: o administrador recebe link de definição de senha por e-mail.
 */
@Service
public class AdministratorManagementUseCase {

    public record AdministratorView(
            UUID accountId,
            String fullName,
            String maskedCpf,
            String email,
            boolean active,
            boolean mfaEnabled,
            Instant lastLoginAt,
            List<RoleRef> roles) {
    }

    public record RoleRef(String code, String name) {
    }

    private final UserAccountRepository accountRepository;
    private final AdministratorRepository administratorRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetTokenIssuer tokenIssuer;
    private final PasswordEncoder passwordEncoder;
    private final AccountSessionRegistry sessionRegistry;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;
    private final AppProperties.Security securityProperties;
    private final Clock clock;

    public AdministratorManagementUseCase(
            UserAccountRepository accountRepository,
            AdministratorRepository administratorRepository,
            RoleRepository roleRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordResetTokenIssuer tokenIssuer,
            PasswordEncoder passwordEncoder,
            AccountSessionRegistry sessionRegistry,
            AuditService auditService,
            JdbcTemplate jdbcTemplate,
            AppProperties appProperties,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.administratorRepository = administratorRepository;
        this.roleRepository = roleRepository;
        this.tokenRepository = tokenRepository;
        this.tokenIssuer = tokenIssuer;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
        this.auditService = auditService;
        this.jdbcTemplate = jdbcTemplate;
        this.securityProperties = appProperties.security();
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdministratorView> list() {
        List<UserAccount> accounts = accountRepository.findByAccountTypeOrderByCreatedAtAsc(AccountType.ADMIN);
        Map<UUID, String> names = administratorRepository
                .findByUserAccountIdIn(accounts.stream().map(UserAccount::getId).toList()).stream()
                .collect(Collectors.toMap(Administrator::getUserAccountId, Administrator::getFullName));
        Map<String, String> roleNames = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getCode, Role::getName));
        // Vínculos de todos os administradores em uma consulta (tabela pequena), evitando N+1.
        Map<UUID, List<String>> rolesByAccount = jdbcTemplate.query(
                        "select user_account_id, role_code from user_account_role",
                        (row, index) -> Map.entry(row.getObject(1, UUID.class), row.getString(2)))
                .stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        return accounts.stream()
                .map(account -> new AdministratorView(
                        account.getId(), names.get(account.getId()), Cpf.mask(account.getCpf()), account.getEmail(),
                        account.isActive(), account.isMfaEnabled(), account.getLastLoginAt(),
                        rolesByAccount.getOrDefault(account.getId(), List.of()).stream()
                                .map(code -> new RoleRef(code, roleNames.get(code)))
                                .toList()))
                .toList();
    }

    /** Cria a conta sem perfis e envia o link de definição de senha. Perfis são atribuídos à parte. */
    @Transactional
    public UUID create(String cpf, String fullName, String email, UUID actorId, String ipAddress) {
        Cpf validCpf = Cpf.of(cpf);
        String normalizedEmail = UserAccount.normalizeEmail(email);
        if (accountRepository.existsByAccountTypeAndCpf(AccountType.ADMIN, validCpf.digits())
                || accountRepository.existsByAccountTypeAndEmail(AccountType.ADMIN, normalizedEmail)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Já existe administrador com este CPF ou e-mail.");
        }
        Instant now = clock.instant();
        // Senha aleatória descartada: a conta só é usada após a definição pelo link.
        String unusablePasswordHash = passwordEncoder.encode(UUID.randomUUID() + UUID.randomUUID().toString());
        UserAccount account = accountRepository.saveAndFlush(
                new UserAccount(AccountType.ADMIN, validCpf, normalizedEmail, unusablePasswordHash, now));
        administratorRepository.save(new Administrator(account.getId(), fullName, now));
        tokenIssuer.issue(account, PasswordResetToken.Purpose.PASSWORD_SETUP,
                securityProperties.passwordSetupTokenValidity(), now);
        audit("ADMINISTRATOR_CREATED", account.getId(), actorId, ipAddress, Map.of());
        return account.getId();
    }

    @Transactional
    public void resendSetupLink(UUID accountId, UUID actorId, String ipAddress) {
        UserAccount account = adminAccount(accountId);
        if (!account.isActive()) {
            throw new DomainRuleException("Reative o administrador antes de reenviar o link.");
        }
        tokenIssuer.issue(account, PasswordResetToken.Purpose.PASSWORD_SETUP,
                securityProperties.passwordSetupTokenValidity(), clock.instant());
        audit("ADMINISTRATOR_SETUP_LINK_SENT", accountId, actorId, ipAddress, Map.of());
    }

    /** Substitui os perfis. As sessões do administrador são encerradas para valer as novas permissões. */
    @Transactional
    public void replaceRoles(UUID accountId, Set<String> roleCodes, UUID actorId, String ipAddress) {
        if (accountId.equals(actorId)) {
            throw new DomainRuleException("Não é possível alterar os próprios perfis.");
        }
        adminAccount(accountId);
        Set<String> known = roleRepository.findAllById(roleCodes).stream().map(Role::getCode).collect(Collectors.toSet());
        if (!known.equals(roleCodes)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Perfil inexistente informado.");
        }
        Set<String> previous = new TreeSet<>(accountRepository.findRoleCodes(accountId));
        if (previous.contains(Role.GENERAL_ADMINISTRATOR) && !roleCodes.contains(Role.GENERAL_ADMINISTRATOR)) {
            requireAnotherGeneralAdministrator(accountId);
        }
        accountRepository.removeRoles(accountId);
        roleCodes.forEach(code -> accountRepository.assignRole(accountId, code));
        sessionRegistry.terminateAllSessions(accountId);
        audit("ADMINISTRATOR_ROLES_CHANGED", accountId, actorId, ipAddress,
                Map.of("from", previous, "to", new TreeSet<>(roleCodes)));
    }

    @Transactional
    public void deactivate(UUID accountId, UUID actorId, String ipAddress) {
        if (accountId.equals(actorId)) {
            throw new DomainRuleException("Não é possível desativar a própria conta.");
        }
        UserAccount account = adminAccount(accountId);
        if (accountRepository.findRoleCodes(accountId).contains(Role.GENERAL_ADMINISTRATOR)) {
            requireAnotherGeneralAdministrator(accountId);
        }
        Instant now = clock.instant();
        account.deactivate(now);
        tokenRepository.invalidateAll(accountId, now);
        sessionRegistry.terminateAllSessions(accountId);
        audit("ADMINISTRATOR_DEACTIVATED", accountId, actorId, ipAddress, Map.of());
    }

    @Transactional
    public void activate(UUID accountId, UUID actorId, String ipAddress) {
        adminAccount(accountId).activate(clock.instant());
        audit("ADMINISTRATOR_ACTIVATED", accountId, actorId, ipAddress, Map.of());
    }

    /** Exige novo cadastro do aplicativo autenticador (ex.: celular perdido). */
    @Transactional
    public void resetMfa(UUID accountId, UUID actorId, String ipAddress) {
        if (accountId.equals(actorId)) {
            throw new DomainRuleException("O segundo fator da própria conta não pode ser redefinido por aqui.");
        }
        adminAccount(accountId).resetMfa(clock.instant());
        sessionRegistry.terminateAllSessions(accountId);
        audit("ADMINISTRATOR_MFA_RESET", accountId, actorId, ipAddress, Map.of());
    }

    private void requireAnotherGeneralAdministrator(UUID accountId) {
        if (accountRepository.countActiveWithRoleExcluding(Role.GENERAL_ADMINISTRATOR, accountId) == 0) {
            throw new DomainRuleException("Deve existir ao menos um Administrador Geral ativo.");
        }
    }

    private UserAccount adminAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .filter(account -> account.getAccountType() == AccountType.ADMIN)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Administrador não encontrado."));
    }

    private void audit(String action, UUID targetAccountId, UUID actorId, String ipAddress, Map<String, ?> details) {
        auditService.record(new AuditService.Entry(action, AuditService.Outcome.SUCCESS, actorId, "USER_ACCOUNT",
                targetAccountId.toString(), ipAddress, details));
    }
}
