package br.gov.pmps.processoseletivo.domain.model.application;

/**
 * Situação da inscrição (ESPECIFICACAO §21; sem "Em análise", docs/DECISOES.md).
 * RASCUNHO ainda não é inscrição: não tem número nem snapshot.
 */
public enum ApplicationStatus {
    RASCUNHO,
    RECEBIDA,
    DEFERIDA,
    INDEFERIDA
}
