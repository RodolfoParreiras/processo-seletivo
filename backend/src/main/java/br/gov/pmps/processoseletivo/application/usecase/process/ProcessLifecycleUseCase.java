package br.gov.pmps.processoseletivo.application.usecase.process;

import br.gov.pmps.processoseletivo.domain.model.process.ProcessNotice;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import br.gov.pmps.processoseletivo.domain.model.process.StatusChange;
import br.gov.pmps.processoseletivo.domain.repository.ProcessNoticeRepository;
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
    private final ProcessSupport support;
    private final Clock clock;

    public ProcessLifecycleUseCase(ProcessNoticeRepository noticeRepository, ProcessSupport support, Clock clock) {
        this.noticeRepository = noticeRepository;
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

        // Se o início das inscrições já chegou, abre imediatamente em vez de esperar a rotina automática.
        process.advanceBySchedule(now)
                .ifPresent(opening -> support.recordStatusChange(process, opening, null, null, ipAddress, now));
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
