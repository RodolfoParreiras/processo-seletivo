package br.gov.pmps.processoseletivo.application.usecase.process;

import br.gov.pmps.processoseletivo.application.dto.process.DocumentRequirementRequest;
import br.gov.pmps.processoseletivo.application.dto.process.PositionRequest;
import br.gov.pmps.processoseletivo.application.dto.process.ProcessDetailsRequest;
import br.gov.pmps.processoseletivo.domain.model.process.DocumentRequirement;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessStatusHistory;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import br.gov.pmps.processoseletivo.domain.model.process.StatusChange;
import br.gov.pmps.processoseletivo.domain.repository.ProcessStatusHistoryRepository;
import br.gov.pmps.processoseletivo.domain.repository.SelectionProcessRepository;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Criação e edição de processos em rascunho (ESPECIFICACAO §80: cria, configura, cadastra cargos). */
@Service
public class ManageDraftProcessUseCase {

    static final String DUPLICATED_NUMBER = "Já existe processo com este número e ano.";

    private final SelectionProcessRepository processRepository;
    private final ProcessStatusHistoryRepository historyRepository;
    private final ProcessSupport support;
    private final Clock clock;

    public ManageDraftProcessUseCase(
            SelectionProcessRepository processRepository,
            ProcessStatusHistoryRepository historyRepository,
            ProcessSupport support,
            Clock clock) {
        this.processRepository = processRepository;
        this.historyRepository = historyRepository;
        this.support = support;
        this.clock = clock;
    }

    @Transactional
    public UUID create(ProcessDetailsRequest request, UUID actorId, String ipAddress) {
        if (processRepository.existsByNumberIgnoreCaseAndYear(request.number().trim(), request.year())) {
            throw new BusinessException(HttpStatus.CONFLICT, DUPLICATED_NUMBER);
        }
        Instant now = clock.instant();
        SelectionProcess process = processRepository.save(new SelectionProcess(toDetails(request), now));
        historyRepository.save(new ProcessStatusHistory(
                process.getId(), new StatusChange(null, process.getStatus()), null, actorId, now));
        support.audit("PROCESS_CREATED", process, actorId, ipAddress, Map.of());
        return process.getId();
    }

    @Transactional
    public void updateDetails(UUID processId, ProcessDetailsRequest request, UUID actorId, String ipAddress) {
        if (processRepository.existsByNumberIgnoreCaseAndYearAndIdNot(request.number().trim(), request.year(), processId)) {
            throw new BusinessException(HttpStatus.CONFLICT, DUPLICATED_NUMBER);
        }
        SelectionProcess process = support.find(processId);
        process.updateDetails(toDetails(request), clock.instant());
        support.audit("PROCESS_UPDATED", process, actorId, ipAddress, Map.of());
    }

    @Transactional
    public UUID addPosition(UUID processId, PositionRequest request, UUID actorId, String ipAddress) {
        SelectionProcess process = support.find(processId);
        UUID positionId = process.addPosition(request.name(), request.vacancies(), clock.instant()).getId();
        support.audit("POSITION_ADDED", process, actorId, ipAddress, Map.of("positionId", positionId.toString()));
        return positionId;
    }

    @Transactional
    public void updatePosition(UUID processId, UUID positionId, PositionRequest request, UUID actorId, String ip) {
        SelectionProcess process = support.find(processId);
        process.updatePosition(positionId, request.name(), request.vacancies(), clock.instant());
        support.audit("POSITION_UPDATED", process, actorId, ip, Map.of("positionId", positionId.toString()));
    }

    @Transactional
    public void removePosition(UUID processId, UUID positionId, UUID actorId, String ipAddress) {
        SelectionProcess process = support.find(processId);
        process.removePosition(positionId, clock.instant());
        support.audit("POSITION_REMOVED", process, actorId, ipAddress, Map.of("positionId", positionId.toString()));
    }

    @Transactional
    public UUID addDocumentRequirement(
            UUID processId, DocumentRequirementRequest request, UUID actorId, String ipAddress) {
        SelectionProcess process = support.find(processId);
        UUID requirementId = process.addDocumentRequirement(toDefinition(request), clock.instant()).getId();
        support.audit("DOCUMENT_REQUIREMENT_ADDED", process, actorId, ipAddress,
                Map.of("requirementId", requirementId.toString()));
        return requirementId;
    }

    @Transactional
    public void updateDocumentRequirement(
            UUID processId, UUID requirementId, DocumentRequirementRequest request, UUID actorId, String ip) {
        SelectionProcess process = support.find(processId);
        process.updateDocumentRequirement(requirementId, toDefinition(request), clock.instant());
        support.audit("DOCUMENT_REQUIREMENT_UPDATED", process, actorId, ip,
                Map.of("requirementId", requirementId.toString()));
    }

    @Transactional
    public void removeDocumentRequirement(UUID processId, UUID requirementId, UUID actorId, String ipAddress) {
        SelectionProcess process = support.find(processId);
        process.removeDocumentRequirement(requirementId, clock.instant());
        support.audit("DOCUMENT_REQUIREMENT_REMOVED", process, actorId, ipAddress,
                Map.of("requirementId", requirementId.toString()));
    }

    private static SelectionProcess.Details toDetails(ProcessDetailsRequest request) {
        return new SelectionProcess.Details(
                request.number(), request.year(), request.title(), request.department(),
                request.registrationStart(), request.registrationEnd(),
                request.multipleApplicationsAllowed(), request.titleEvaluationEnabled());
    }

    private static DocumentRequirement.Definition toDefinition(DocumentRequirementRequest request) {
        return new DocumentRequirement.Definition(
                request.name(), request.description(), request.mandatory(), request.title());
    }
}
