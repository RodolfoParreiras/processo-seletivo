package br.gov.pmps.processoseletivo.application.dto.process;

import br.gov.pmps.processoseletivo.domain.model.process.ProcessStatus;
import java.time.Instant;

/** {@code changedBy} nulo indica mudança automática pelas datas do período. */
public record StatusHistoryResponse(
        ProcessStatus fromStatus, ProcessStatus toStatus, String reason, String changedBy, Instant changedAt) {
}
