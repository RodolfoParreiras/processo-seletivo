package br.gov.pmps.processoseletivo.application.usecase.application;

import br.gov.pmps.processoseletivo.domain.model.application.Application;
import br.gov.pmps.processoseletivo.domain.repository.ApplicationRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Descarta rascunhos não confirmados de processos que não aceitam mais inscrições (docs/DECISOES.md).
 * Rascunho não é inscrição: não tem número, snapshot nem valor histórico.
 */
@Service
public class DiscardExpiredDraftsUseCase {

    private static final Logger log = LoggerFactory.getLogger(DiscardExpiredDraftsUseCase.class);

    private final ApplicationRepository applicationRepository;
    private final ApplicationDraftUseCase draftUseCase;
    private final ApplicationSupport support;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public DiscardExpiredDraftsUseCase(
            ApplicationRepository applicationRepository,
            ApplicationDraftUseCase draftUseCase,
            ApplicationSupport support,
            TransactionTemplate transactionTemplate,
            Clock clock) {
        this.applicationRepository = applicationRepository;
        this.draftUseCase = draftUseCase;
        this.support = support;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /** @return quantidade de rascunhos descartados */
    public int execute() {
        List<UUID> expired = applicationRepository.findExpiredDraftIds(clock.instant());
        int discarded = 0;
        for (UUID applicationId : expired) {
            try {
                Boolean removed = transactionTemplate.execute(status -> discard(applicationId));
                discarded += Boolean.TRUE.equals(removed) ? 1 : 0;
            } catch (RuntimeException exception) {
                log.error("Falha ao descartar rascunho {}", applicationId, exception);
            }
        }
        return discarded;
    }

    private boolean discard(UUID applicationId) {
        Application application = applicationRepository.findById(applicationId).orElse(null);
        // Pode ter sido confirmado ou removido entre a consulta e esta transação.
        if (application == null || !application.isDraft()) {
            return false;
        }
        draftUseCase.deleteDraft(application);
        support.audit("APPLICATION_DRAFT_EXPIRED", application, null, null, Map.of());
        return true;
    }
}
