package br.gov.pmps.processoseletivo.domain.model.process;

import java.util.EnumSet;
import java.util.Set;

/** Ciclo de vida do processo seletivo (ESPECIFICACAO §14 e docs/DECISOES.md). */
public enum ProcessStatus {
    RASCUNHO,
    PUBLICADO,
    INSCRICOES_ABERTAS,
    INSCRICOES_ENCERRADAS,
    RESULTADO_PRELIMINAR,
    RESULTADO_DEFINITIVO,
    ARQUIVADO,
    SUSPENSO,
    CANCELADO;

    private static final Set<ProcessStatus> SUSPENDABLE = EnumSet.of(
            PUBLICADO, INSCRICOES_ABERTAS, INSCRICOES_ENCERRADAS, RESULTADO_PRELIMINAR, RESULTADO_DEFINITIVO);

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
