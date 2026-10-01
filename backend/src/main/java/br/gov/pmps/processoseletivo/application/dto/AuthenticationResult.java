package br.gov.pmps.processoseletivo.application.dto;

import br.gov.pmps.processoseletivo.domain.model.AccountType;
import java.util.Set;
import java.util.UUID;

public record AuthenticationResult(UUID accountId, AccountType accountType, String displayName, Set<String> permissions) {
}
