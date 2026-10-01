package br.gov.pmps.processoseletivo.shared.error;

/**
 * Regra de domínio violada pelo estado atual (ex.: editar processo já publicado).
 * Respondida como 409. A mensagem é exibida ao usuário.
 */
public class DomainRuleException extends RuntimeException {

    public DomainRuleException(String message) {
        super(message);
    }
}
