package br.gov.pmps.processoseletivo.application.dto.process;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReasonRequest(
        @NotBlank(message = "Informe o motivo.") @Size(max = 1000, message = "Máximo de 1000 caracteres.")
        String reason) {
}
