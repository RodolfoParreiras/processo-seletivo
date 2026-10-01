package br.gov.pmps.processoseletivo.application.usecase.process;

import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import br.gov.pmps.processoseletivo.domain.repository.SelectionProcessRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Abre e encerra inscrições conforme as datas (docs/DECISOES.md). Cada processo usa sua própria transação
 * com bloqueio da linha, para que execuções simultâneas (várias instâncias) não dupliquem a transição.
 */
@Service
public class AdvanceProcessStatusesUseCase {

    private static final Logger log = LoggerFactory.getLogger(AdvanceProcessStatusesUseCase.class);

    private final SelectionProcessRepository processRepository;
    private final ProcessSupport support;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public AdvanceProcessStatusesUseCase(
            SelectionProcessRepository processRepository,
            ProcessSupport support,
            TransactionTemplate transactionTemplate,
            Clock clock) {
        this.processRepository = processRepository;
        this.support = support;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /** @return quantidade de transições realizadas */
    public int execute() {
        Instant now = clock.instant();
        List<UUID> dueProcessIds = processRepository.findIdsDueForScheduledTransition(now);
        int transitions = 0;
        for (UUID processId : dueProcessIds) {
            try {
                Integer applied = transactionTemplate.execute(status -> advance(processId, now));
                transitions += applied == null ? 0 : applied;
            } catch (RuntimeException exception) {
                // Falha em um processo não impede os demais; a próxima execução tenta novamente.
                log.error("Falha na transição automática do processo {}", processId, exception);
            }
        }
        return transitions;
    }

    private int advance(UUID processId, Instant now) {
        SelectionProcess process = support.findForUpdate(processId);
        int applied = 0;
        // Um processo publicado cujo período inteiro já passou abre e encerra na mesma execução.
        for (var change = process.advanceBySchedule(now); change.isPresent(); change = process.advanceBySchedule(now)) {
            support.recordStatusChange(process, change.get(), null, null, null, now);
            applied++;
        }
        return applied;
    }
}
