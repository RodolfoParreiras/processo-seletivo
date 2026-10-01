package br.gov.pmps.processoseletivo.application.dto;

import br.gov.pmps.processoseletivo.domain.model.AccountType;
import java.util.Set;
import java.util.UUID;

/** {@code mfaEnabled} só se aplica a contas administrativas, que exigem segundo fator. */
public record AuthenticationResult(
        UUID accountId, AccountType accountType, String displayName, Set<String> permissions, boolean mfaEnabled) {

    public AuthenticationResult(UUID accountId, AccountType accountType, String displayName, Set<String> permissions) {
        this(accountId, accountType, displayName, permissions, false);
    }
}
