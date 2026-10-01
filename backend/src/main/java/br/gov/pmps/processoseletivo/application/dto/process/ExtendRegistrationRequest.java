package br.gov.pmps.processoseletivo.application.dto.process;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ExtendRegistrationRequest(
        @NotNull(message = "Informe a nova data final.") Instant newRegistrationEnd,
        @NotBlank(message = "Informe o motivo.") @Size(max = 1000, message = "Máximo de 1000 caracteres.")
        String reason) {
}
