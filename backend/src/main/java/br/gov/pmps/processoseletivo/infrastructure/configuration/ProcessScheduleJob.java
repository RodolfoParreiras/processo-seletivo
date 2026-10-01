package br.gov.pmps.processoseletivo.infrastructure.configuration;

import br.gov.pmps.processoseletivo.application.usecase.process.AdvanceProcessStatusesUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Executa a abertura/encerramento automático das inscrições. Desligável por configuração (ex.: testes). */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ProcessScheduleJob {

    private final AdvanceProcessStatusesUseCase advanceProcessStatuses;

    public ProcessScheduleJob(AdvanceProcessStatusesUseCase advanceProcessStatuses) {
        this.advanceProcessStatuses = advanceProcessStatuses;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT10S")
    void advanceProcessStatuses() {
        advanceProcessStatuses.execute();
    }
}
