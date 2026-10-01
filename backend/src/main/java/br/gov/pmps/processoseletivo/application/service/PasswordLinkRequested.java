package br.gov.pmps.processoseletivo.application.service;

import br.gov.pmps.processoseletivo.domain.model.PasswordResetToken;

/**
 * Publicado quando um link de definição/redefinição de senha precisa ser enviado.
 * O token em claro existe só neste evento em memória; não deve ser persistido nem registrado em log.
 */
public record PasswordLinkRequested(String email, String rawToken, PasswordResetToken.Purpose purpose) {

    @Override
    public String toString() {
        return "PasswordLinkRequested[purpose=" + purpose + "]";
    }
}
