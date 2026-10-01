package br.gov.pmps.processoseletivo.application.service;

/**
 * Aviso ao titular sobre alteração sensível na conta, para que perceba uso indevido.
 * No caso de troca de e-mail, o aviso vai para o endereço anterior.
 */
public record AccountSecurityNotice(String email, Type type) {

    public enum Type {
        EMAIL_CHANGED,
        PASSWORD_CHANGED
    }

    @Override
    public String toString() {
        return "AccountSecurityNotice[type=" + type + "]";
    }
}
