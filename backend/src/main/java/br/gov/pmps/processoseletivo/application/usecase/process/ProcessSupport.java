package br.gov.pmps.processoseletivo.application.usecase.process;

import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessStatusHistory;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import br.gov.pmps.processoseletivo.domain.model.process.StatusChange;
import br.gov.pmps.processoseletivo.domain.repository.ProcessStatusHistoryRepository;
import br.gov.pmps.processoseletivo.domain.repository.SelectionProcessRepository;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Operações comuns aos casos de uso de processo: localizar e registrar mudanças de situação. */
@Component
public class ProcessSupport {

    static final String TARGET_TYPE = "SELECTION_PROCESS";

    private final SelectionProcessRepository processRepository;
    private final ProcessStatusHistoryRepository historyRepository;
    private final AuditService auditService;

    public ProcessSupport(
            SelectionProcessRepository processRepository,
            ProcessStatusHistoryRepository historyRepository,
            AuditService auditService) {
        this.processRepository = processRepository;
        this.historyRepository = historyRepository;
        this.auditService = auditService;
    }

    public SelectionProcess find(UUID processId) {
        return processRepository.findById(processId).orElseThrow(ProcessSupport::notFound);
    }

    /** Bloqueia a linha do processo: serializa operações que mudam situação ou edital. */
    public SelectionProcess findForUpdate(UUID processId) {
        return processRepository.findForUpdate(processId).orElseThrow(ProcessSupport::notFound);
    }

    /** @param actorAccountId {@code null} para transições automáticas */
    public void recordStatusChange(
            SelectionProcess process, StatusChange change, String reason, UUID actorAccountId, String ipAddress,
            Instant now) {
        historyRepository.save(new ProcessStatusHistory(process.getId(), change, reason, actorAccountId, now));
        Map<String, Object> details = new HashMap<>();
        details.put("from", change.from().name());
        details.put("to", change.to().name());
        if (actorAccountId == null) {
            details.put("automatic", true);
        }
        audit("PROCESS_STATUS_CHANGED", process, actorAccountId, ipAddress, details);
    }

    public void audit(
            String action, SelectionProcess process, UUID actorAccountId, String ipAddress, Map<String, ?> details) {
        auditService.record(new AuditService.Entry(
                action, AuditService.Outcome.SUCCESS, actorAccountId, TARGET_TYPE,
                process.getId().toString(), ipAddress, details));
    }

    public static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "Processo seletivo não encontrado.");
    }
}
