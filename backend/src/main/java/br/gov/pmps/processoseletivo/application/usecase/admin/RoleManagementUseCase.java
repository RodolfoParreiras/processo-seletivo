package br.gov.pmps.processoseletivo.application.usecase.admin;

import br.gov.pmps.processoseletivo.application.service.AccountSessionRegistry;
import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.domain.model.Permission;
import br.gov.pmps.processoseletivo.domain.model.Role;
import br.gov.pmps.processoseletivo.domain.repository.PermissionRepository;
import br.gov.pmps.processoseletivo.domain.repository.RoleRepository;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import br.gov.pmps.processoseletivo.shared.error.DomainRuleException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Perfis e permissões (ESPECIFICACAO §6 e §5.3). As permissões em si são fixas no sistema. */
@Service
public class RoleManagementUseCase {

    public record PermissionView(String code, String description) {
    }

    public record RoleView(String code, String name, boolean systemRole, Set<String> permissions, int members) {
    }

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AccountSessionRegistry sessionRegistry;
    private final AuditService auditService;

    public RoleManagementUseCase(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            AccountSessionRegistry sessionRegistry,
            AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.sessionRegistry = sessionRegistry;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PermissionView> permissions() {
        return permissionRepository.findAllByOrderByCodeAsc().stream()
                .map(permission -> new PermissionView(permission.getCode(), permission.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleView> roles() {
        return roleRepository.findAllByOrderByNameAsc().stream()
                .map(role -> new RoleView(role.getCode(), role.getName(), role.isSystemRole(),
                        new TreeSet<>(role.getPermissionCodes()), roleRepository.findMemberAccountIds(role.getCode()).size()))
                .toList();
    }

    @Transactional
    public String create(String name, Set<String> permissionCodes, UUID actorId, String ipAddress) {
        requireUniqueName(name, "");
        requireKnownPermissions(permissionCodes);
        Role role = roleRepository.save(new Role(name, permissionCodes));
        audit("ROLE_CREATED", role.getCode(), actorId, ipAddress,
                Map.of("name", role.getName(), "permissions", new TreeSet<>(permissionCodes)));
        return role.getCode();
    }

    /** Altera nome e permissões. Os membros têm as sessões encerradas para valer as novas permissões. */
    @Transactional
    public void update(String code, String name, Set<String> permissionCodes, UUID actorId, String ipAddress) {
        Role role = find(code);
        requireUniqueName(name, code);
        requireKnownPermissions(permissionCodes);
        Set<String> previous = new TreeSet<>(role.getPermissionCodes());
        role.change(name, permissionCodes);
        roleRepository.findMemberAccountIds(code).forEach(sessionRegistry::terminateAllSessions);
        audit("ROLE_UPDATED", code, actorId, ipAddress,
                Map.of("name", role.getName(), "from", previous, "to", new TreeSet<>(permissionCodes)));
    }

    /** Somente perfis sem administradores vinculados. */
    @Transactional
    public void delete(String code, UUID actorId, String ipAddress) {
        Role role = find(code);
        role.requireEditable();
        if (!roleRepository.findMemberAccountIds(code).isEmpty()) {
            throw new DomainRuleException("Remova o perfil dos administradores antes de excluí-lo.");
        }
        roleRepository.delete(role);
        audit("ROLE_DELETED", code, actorId, ipAddress, Map.of("name", role.getName()));
    }

    private Role find(String code) {
        return roleRepository.findById(code)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Perfil não encontrado."));
    }

    private void requireUniqueName(String name, String ignoredCode) {
        if (roleRepository.existsByNameIgnoringCode(name.trim(), ignoredCode)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Já existe perfil com este nome.");
        }
    }

    private void requireKnownPermissions(Set<String> permissionCodes) {
        Set<String> known = permissionRepository.findAllById(permissionCodes).stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
        if (!known.equals(permissionCodes)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Permissão inexistente informada.");
        }
    }

    private void audit(String action, String roleCode, UUID actorId, String ipAddress, Map<String, ?> details) {
        auditService.record(new AuditService.Entry(action, AuditService.Outcome.SUCCESS, actorId, "ROLE",
                roleCode, ipAddress, details));
    }
}
