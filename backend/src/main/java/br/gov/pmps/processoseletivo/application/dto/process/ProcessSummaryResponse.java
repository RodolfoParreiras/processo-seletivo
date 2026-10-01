package br.gov.pmps.processoseletivo.application.dto.process;

import br.gov.pmps.processoseletivo.domain.model.process.ProcessStatus;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import java.time.Instant;
import java.util.UUID;

public record ProcessSummaryResponse(
        UUID id,
        String number,
        int year,
        String displayNumber,
        String title,
        String department,
        ProcessStatus status,
        Instant registrationStart,
        Instant registrationEnd) {

    public static ProcessSummaryResponse from(SelectionProcess process) {
        return new ProcessSummaryResponse(
                process.getId(), process.getNumber(), process.getYear(), process.getDisplayNumber(), process.getTitle(),
                process.getDepartment(), process.getStatus(), process.getRegistrationStart(),
                process.getRegistrationEnd());
    }
}
