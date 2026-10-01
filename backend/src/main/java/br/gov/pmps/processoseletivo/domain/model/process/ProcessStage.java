package br.gov.pmps.processoseletivo.domain.model.process;

/**
 * Etapa de divulgação do processo, definida manualmente pelo administrador a partir de lista fixa
 * (docs/DECISOES.md). Independe da situação automática das inscrições.
 */
public enum ProcessStage {
    EDITAL_DISPONIVEL,
    GABARITO_DISPONIVEL,
    RESULTADO_PRELIMINAR,
    RESULTADO_FINAL
}
