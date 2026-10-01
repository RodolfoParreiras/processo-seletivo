package br.gov.pmps.processoseletivo.application.service;

import java.util.UUID;

/** Encerra sessões ativas de uma conta, por exemplo após troca de senha (ESPECIFICACAO §12). */
public interface AccountSessionRegistry {

    void terminateAllSessions(UUID accountId);

    /** Encerra as demais sessões, mantendo a sessão de quem fez a alteração. */
    void terminateOtherSessions(UUID accountId, String currentSessionId);
}
