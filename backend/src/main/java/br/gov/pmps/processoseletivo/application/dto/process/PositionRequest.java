package br.gov.pmps.processoseletivo.application.dto.process;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PositionRequest(
        @NotBlank(message = "Informe o nome do cargo.") @Size(max = 150, message = "Máximo de 150 caracteres.")
        String name,
        @NotNull(message = "Informe a quantidade de vagas.") @Positive(message = "Informe ao menos uma vaga.")
        @Max(value = 100000, message = "Quantidade de vagas inválida.")
        Integer vacancies) {
}
