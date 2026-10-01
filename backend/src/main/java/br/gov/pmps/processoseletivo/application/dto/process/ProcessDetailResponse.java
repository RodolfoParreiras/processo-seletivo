package br.gov.pmps.processoseletivo.application.dto.process;

import br.gov.pmps.processoseletivo.domain.model.process.DocumentRequirement;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessNotice;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessPosition;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessStage;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessStatus;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProcessDetailResponse(
        UUID id,
        String number,
        int year,
        String displayNumber,
        String title,
        String department,
        ProcessStatus status,
        ProcessStage stage,
        Instant registrationStart,
        Instant registrationEnd,
        boolean multipleApplicationsAllowed,
        boolean titleEvaluationEnabled,
        Instant publishedAt,
        List<Position> positions,
        List<Requirement> documentRequirements,
        List<Notice> notices) {

    public record Position(UUID id, String name, int vacancies) {
    }

    public record Requirement(UUID id, String name, String description, boolean mandatory, boolean title) {
    }

    public record Notice(int version, ProcessNotice.Status status, String changeReason, Instant publishedAt) {
    }

    public static ProcessDetailResponse from(SelectionProcess process, List<ProcessNotice> notices) {
        return new ProcessDetailResponse(
                process.getId(), process.getNumber(), process.getYear(), process.getDisplayNumber(), process.getTitle(),
                process.getDepartment(), process.getStatus(), process.getStage(), process.getRegistrationStart(),
                process.getRegistrationEnd(), process.isMultipleApplicationsAllowed(),
                process.isTitleEvaluationEnabled(), process.getPublishedAt(),
                process.getPositions().stream().map(ProcessDetailResponse::toPosition).toList(),
                process.getDocumentRequirements().stream().map(ProcessDetailResponse::toRequirement).toList(),
                notices.stream().map(ProcessDetailResponse::toNotice).toList());
    }

    private static Position toPosition(ProcessPosition position) {
        return new Position(position.getId(), position.getName(), position.getVacancies());
    }

    private static Requirement toRequirement(DocumentRequirement requirement) {
        return new Requirement(requirement.getId(), requirement.getName(), requirement.getDescription(),
                requirement.isMandatory(), requirement.isTitle());
    }

    private static Notice toNotice(ProcessNotice notice) {
        return new Notice(
                notice.getNoticeVersion(), notice.getStatus(), notice.getChangeReason(), notice.getPublishedAt());
    }
}
