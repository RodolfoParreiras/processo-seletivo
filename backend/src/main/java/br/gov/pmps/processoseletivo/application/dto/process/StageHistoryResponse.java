package br.gov.pmps.processoseletivo.application.dto.process;

import br.gov.pmps.processoseletivo.domain.model.process.ProcessStage;
import java.time.Instant;

public record StageHistoryResponse(ProcessStage fromStage, ProcessStage toStage, String changedBy, Instant changedAt) {
}
