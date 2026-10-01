package br.gov.pmps.processoseletivo.application.usecase.process;

import br.gov.pmps.processoseletivo.domain.model.process.ProcessNotice;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessStage;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessStageHistory;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import br.gov.pmps.processoseletivo.domain.model.process.StatusChange;
import br.gov.pmps.processoseletivo.domain.repository.ProcessNoticeRepository;
import br.gov.pmps.processoseletivo.domain.repository.ProcessStageHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Publicação, prorrogação, suspensão, retomada, cancelamento e arquivamento (ESPECIFICACAO §14). */
@Service
public class ProcessLifecycleUseCase {

    private final ProcessNoticeRepository noticeRepository;
    private final ProcessStageHistoryRepository stageHistoryRepository;
    private final ProcessSupport support;
    private final Clock clock;

    public ProcessLifecycleUseCase(
            ProcessNoticeRepository noticeRepository,
            ProcessStageHistoryRepository stageHistoryRepository,
            ProcessSupport support,
            Clock clock) {
        this.noticeRepository = noticeRepository;
        this.stageHistoryRepository = stageHistoryRepository;
        this.support = support;
        this.clock = clock;
    }

    @Transactional
    public void publish(UUID processId, UUID actorId, String ipAddress) {
        Instant now = clock.instant();
        SelectionProcess process = support.findForUpdate(processId);
        Optional<ProcessNotice> draftNotice =
                noticeRepository.findByProcessIdAndStatusIn(processId, EnumSet.of(ProcessNotice.Status.DRAFT));

        StatusChange change = process.publish(draftNotice.isPresent(), now);
        draftNotice.get().publish(actorId, now);
        support.recordStatusChange(process, change, null, actorId, ipAddress, now);
        stageHistoryRepository.save(
                new ProcessStageHistory(processId, null, process.getStage(), actorId, now));

        // Se o início das inscrições já chegou, abre imediatamente em vez de esperar a rotina automática.
        process.advanceBySchedule(now)
                .ifPresent(opening -> support.recordStatusChange(process, opening, null, null, ipAddress, now));
    }

    /** Etapa de divulgação (Edital Disponível, Gabarito Disponível, Resultado Preliminar, Resultado Final). */
    @Transactional
    public void changeStage(UUID processId, ProcessStage stage, UUID actorId, String ipAddress) {
        Instant now = clock.instant();
        SelectionProcess process = support.findForUpdate(processId);
        ProcessStage previous = process.changeStage(stage, now);
        stageHistoryRepository.save(new ProcessStageHistory(processId, previous, stage, actorId, now));
        support.audit("PROCESS_STAGE_CHANGED", process, actorId, ipAddress,
                Map.of("from", previous == null ? "" : previous.name(), "to", stage.name()));
    }

    @Transactional
    public void extendRegistration(UUID processId, Instant newEnd, String reason, UUID actorId, String ipAddress) {
        Instant now = clock.instant();
        SelectionProcess process = support.findForUpdate(processId);
        Instant previousEnd = process.getRegistrationEnd();
        process.extendRegistration(newEnd, now);
        support.audit("REGISTRATION_EXTENDED", process, actorId, ipAddress, Map.of(
                "previousEnd", previousEnd.toString(),
                "newEnd", newEnd.toString(),
                "reason", reason.trim()));
    }

    @Transactional
    public void suspend(UUID processId, String reason, UUID actorId, String ipAddress) {
        Instant now = clock.instant();
        SelectionProcess process = support.findForUpdate(processId);
        support.recordStatusChange(process, process.suspend(now), reason, actorId, ipAddress, now);
    }

    @Transactional
    public void resume(UUID processId, String reason, UUID actorId, String ipAddress) {
        Instant now = clock.instant();
        SelectionProcess process = support.findForUpdate(processId);
        support.recordStatusChange(process, process.resume(now), reason, actorId, ipAddress, now);
        // Se o período mudou de fase durante a suspensão, aplica a transição pendente.
        process.advanceBySchedule(now)
                .ifPresent(pending -> support.recordStatusChange(process, pending, null, null, ipAddress, now));
    }

    @Transactional
    public void cancel(UUID processId, String reason, UUID actorId, String ipAddress) {
        Instant now = clock.instant();
        SelectionProcess process = support.findForUpdate(processId);
        support.recordStatusChange(process, process.cancel(now), reason, actorId, ipAddress, now);
    }

    @Transactional
    public void archive(UUID processId, String reason, UUID actorId, String ipAddress) {
        Instant now = clock.instant();
        SelectionProcess process = support.findForUpdate(processId);
        support.recordStatusChange(process, process.archive(now), reason, actorId, ipAddress, now);
    }
}
