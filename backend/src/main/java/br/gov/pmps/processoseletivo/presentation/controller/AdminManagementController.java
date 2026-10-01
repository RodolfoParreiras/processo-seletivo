package br.gov.pmps.processoseletivo.presentation.controller;

import br.gov.pmps.processoseletivo.application.dto.PageResponse;
import br.gov.pmps.processoseletivo.application.dto.ValidCpf;
import br.gov.pmps.processoseletivo.application.usecase.admin.AdministratorManagementUseCase;
import br.gov.pmps.processoseletivo.application.usecase.admin.AuditQueryUseCase;
import br.gov.pmps.processoseletivo.application.usecase.admin.RoleManagementUseCase;
import br.gov.pmps.processoseletivo.security.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Administradores, perfis e auditoria (ESPECIFICACAO §5.3, §6 e §78). */
@RestController
@RequestMapping("/api/admin")
public class AdminManagementController {

    public record CreateAdministratorRequest(
            @NotBlank(message = "Informe o CPF.") @ValidCpf String cpf,
            @NotBlank(message = "Informe o nome.") @Size(max = 150, message = "Máximo de 150 caracteres.") String fullName,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 254) String email) {
    }

    public record RolesRequest(@NotNull(message = "Informe os perfis.") Set<String> roleCodes) {
    }

    public record RoleRequest(
            @NotBlank(message = "Informe o nome.") @Size(max = 100, message = "Máximo de 100 caracteres.") String name,
            @NotNull(message = "Informe as permissões.") Set<String> permissions) {
    }

    private final AdministratorManagementUseCase administrators;
    private final RoleManagementUseCase roles;
    private final AuditQueryUseCase audit;

    public AdminManagementController(
            AdministratorManagementUseCase administrators, RoleManagementUseCase roles, AuditQueryUseCase audit) {
        this.administrators = administrators;
        this.roles = roles;
        this.audit = audit;
    }

    // ---- Administradores ----

    @GetMapping("/administrators")
    @PreAuthorize("hasAuthority('USUARIO_GERENCIAR')")
    List<AdministratorManagementUseCase.AdministratorView> listAdministrators() {
        return administrators.list();
    }

    @PostMapping("/administrators")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USUARIO_GERENCIAR')")
    Map<String, UUID> createAdministrator(
            @Valid @RequestBody CreateAdministratorRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        return Map.of("id", administrators.create(request.cpf(), request.fullName(), request.email(),
                account.accountId(), httpRequest.getRemoteAddr()));
    }

    @PutMapping("/administrators/{accountId}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PERMISSAO_GERENCIAR')")
    void replaceRoles(
            @PathVariable UUID accountId,
            @Valid @RequestBody RolesRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        administrators.replaceRoles(accountId, request.roleCodes(), account.accountId(), httpRequest.getRemoteAddr());
    }

    @PostMapping("/administrators/{accountId}/{operation}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USUARIO_GERENCIAR')")
    void administratorOperation(
            @PathVariable UUID accountId,
            @PathVariable String operation,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        switch (operation) {
            case "deactivate" -> administrators.deactivate(accountId, account.accountId(), ip);
            case "activate" -> administrators.activate(accountId, account.accountId(), ip);
            case "reset-mfa" -> administrators.resetMfa(accountId, account.accountId(), ip);
            case "resend-setup-link" -> administrators.resendSetupLink(accountId, account.accountId(), ip);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    // ---- Perfis e permissões ----

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSAO_GERENCIAR')")
    List<RoleManagementUseCase.PermissionView> permissions() {
        return roles.permissions();
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('PERMISSAO_GERENCIAR', 'USUARIO_GERENCIAR')")
    List<RoleManagementUseCase.RoleView> roles() {
        return roles.roles();
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PERMISSAO_GERENCIAR')")
    Map<String, String> createRole(
            @Valid @RequestBody RoleRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        return Map.of("code", roles.create(request.name(), request.permissions(), account.accountId(),
                httpRequest.getRemoteAddr()));
    }

    @PutMapping("/roles/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PERMISSAO_GERENCIAR')")
    void updateRole(
            @PathVariable String code,
            @Valid @RequestBody RoleRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        roles.update(code, request.name(), request.permissions(), account.accountId(), httpRequest.getRemoteAddr());
    }

    @DeleteMapping("/roles/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PERMISSAO_GERENCIAR')")
    void deleteRole(
            @PathVariable String code,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        roles.delete(code, account.accountId(), httpRequest.getRemoteAddr());
    }

    // ---- Auditoria ----

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('AUDITORIA_VISUALIZAR')")
    PageResponse<AuditQueryUseCase.AuditEntry> auditLog(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) UUID actorAccountId,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return audit.search(new AuditQueryUseCase.Filter(action, outcome, actorAccountId, targetId, from, to), page, size);
    }
}
