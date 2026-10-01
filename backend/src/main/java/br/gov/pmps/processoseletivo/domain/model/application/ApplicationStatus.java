package br.gov.pmps.processoseletivo.domain.model.application;

/** Situação da inscrição (ESPECIFICACAO §21). RASCUNHO ainda não é inscrição: não tem número nem snapshot. */
public enum ApplicationStatus {
    RASCUNHO,
    RECEBIDA,
    EM_ANALISE,
    DEFERIDA,
    INDEFERIDA
}
