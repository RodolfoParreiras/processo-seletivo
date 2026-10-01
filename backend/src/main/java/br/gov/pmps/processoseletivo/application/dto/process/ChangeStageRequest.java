package br.gov.pmps.processoseletivo.application.dto.process;

import br.gov.pmps.processoseletivo.domain.model.process.ProcessStage;
import jakarta.validation.constraints.NotNull;

public record ChangeStageRequest(@NotNull(message = "Informe a etapa.") ProcessStage stage) {
}
