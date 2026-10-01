package br.gov.pmps.processoseletivo.application.dto;

import br.gov.pmps.processoseletivo.domain.model.AccountType;
import java.util.Set;

/** Dados mínimos para a interface montar o menu; a autorização continua sendo do backend. */
public record SessionResponse(AccountType accountType, String displayName, Set<String> permissions) {
}
