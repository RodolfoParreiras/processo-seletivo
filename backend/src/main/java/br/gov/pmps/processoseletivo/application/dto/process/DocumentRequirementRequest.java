package br.gov.pmps.processoseletivo.application.dto.process;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DocumentRequirementRequest(
        @NotBlank(message = "Informe o nome do documento.") @Size(max = 150, message = "Máximo de 150 caracteres.")
        String name,
        @Size(max = 500, message = "Máximo de 500 caracteres.") String description,
        @NotNull(message = "Informe se o documento é obrigatório.") Boolean mandatory,
        @NotNull(message = "Informe se o documento é um título.") Boolean title) {
}
