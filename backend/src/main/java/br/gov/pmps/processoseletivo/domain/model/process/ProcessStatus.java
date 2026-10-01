package br.gov.pmps.processoseletivo.domain.model.process;

import java.util.EnumSet;
import java.util.Set;

/**
 * Situação do processo quanto às inscrições (ESPECIFICACAO §14 e docs/DECISOES.md). O andamento
 * da divulgação (edital, gabarito, resultados) é a {@link ProcessStage}, definida manualmente.
 */
public enum ProcessStatus {
    RASCUNHO,
    PUBLICADO,
    INSCRICOES_ABERTAS,
    INSCRICOES_ENCERRADAS,
    ARQUIVADO,
    SUSPENSO,
    CANCELADO;

    private static final Set<ProcessStatus> SUSPENDABLE = EnumSet.of(PUBLICADO, INSCRICOES_ABERTAS, INSCRICOES_ENCERRADAS);

    public boolean isTerminal() {
        return this == ARQUIVADO || this == CANCELADO;
    }

    /** Rascunhos nunca aparecem fora da área administrativa. */
    public boolean isPubliclyVisible() {
        return this != RASCUNHO;
    }

    public boolean isSuspendable() {
        return SUSPENDABLE.contains(this);
    }
}
