package br.gov.pmps.processoseletivo.security;

import br.gov.pmps.processoseletivo.domain.model.AccountType;
import java.io.Serializable;
import java.util.UUID;
import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * Principal guardado na sessão. O nome do principal é o id interno da conta, para que
 * a tabela de sessões não armazene CPF ou e-mail.
 */
public record AuthenticatedAccount(UUID accountId, AccountType accountType, String displayName)
        implements AuthenticatedPrincipal, Serializable {

    @Override
    public String getName() {
        return accountId.toString();
    }
}
