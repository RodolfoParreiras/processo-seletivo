package br.gov.pmps.processoseletivo.domain.model;

import br.gov.pmps.processoseletivo.shared.error.DomainRuleException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Perfil administrativo: conjunto de permissões (ESPECIFICACAO §6). Perfis do sistema não são editáveis. */
@Entity
@Table(name = "role")
public class Role {

    public static final String GENERAL_ADMINISTRATOR = "ADMINISTRADOR_GERAL";

    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "system_role", nullable = false)
    private boolean systemRole;

    @ElementCollection
    @CollectionTable(name = "role_permission", joinColumns = @JoinColumn(name = "role_code"))
    @Column(name = "permission_code", nullable = false)
    private Set<String> permissionCodes = new HashSet<>();

    protected Role() {
    }

    public Role(String name, Set<String> permissionCodes) {
        // Código interno gerado: o nome exibido pode mudar sem afetar vínculos.
        this.code = "PERFIL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        this.name = name.trim();
        this.permissionCodes = new HashSet<>(permissionCodes);
    }

    public void change(String newName, Set<String> newPermissionCodes) {
        requireEditable();
        name = newName.trim();
        permissionCodes.clear();
        permissionCodes.addAll(newPermissionCodes);
    }

    public void requireEditable() {
        if (systemRole) {
            throw new DomainRuleException("Perfis do sistema não podem ser alterados.");
        }
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isSystemRole() {
        return systemRole;
    }

    public Set<String> getPermissionCodes() {
        return Set.copyOf(permissionCodes);
    }
}
