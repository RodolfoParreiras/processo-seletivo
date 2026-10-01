package br.gov.pmps.processoseletivo.infrastructure.configuration;

import br.gov.pmps.processoseletivo.application.service.EmailOutboxService;
import br.gov.pmps.processoseletivo.application.usecase.application.DiscardExpiredDraftsUseCase;
import br.gov.pmps.processoseletivo.application.usecase.process.AdvanceProcessStatusesUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Rotinas periódicas. Desligáveis por configuração (ex.: testes, que chamam os casos de uso diretamente). */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledJobs {

    private final AdvanceProcessStatusesUseCase advanceProcessStatuses;
    private final DiscardExpiredDraftsUseCase discardExpiredDrafts;
    private final EmailOutboxService emailOutbox;

    public ScheduledJobs(
            AdvanceProcessStatusesUseCase advanceProcessStatuses,
            DiscardExpiredDraftsUseCase discardExpiredDrafts,
            EmailOutboxService emailOutbox) {
        this.advanceProcessStatuses = advanceProcessStatuses;
        this.discardExpiredDrafts = discardExpiredDrafts;
        this.emailOutbox = emailOutbox;
    }

    /** Abertura e encerramento automático das inscrições pelas datas. */
    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT10S")
    void advanceProcessStatuses() {
        advanceProcessStatuses.execute();
    }

    @Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT1M")
    void discardExpiredDrafts() {
        discardExpiredDrafts.execute();
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT20S")
    void dispatchEmails() {
        emailOutbox.dispatchDue();
    }
}
