package br.gov.pmps.processoseletivo.application.dto.process;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ProcessDetailsRequest(
        @NotBlank(message = "Informe o número.") @Size(max = 20, message = "Máximo de 20 caracteres.")
        @Pattern(regexp = "[0-9A-Za-z./-]+", message = "Use apenas letras, números, ponto, barra ou hífen.")
        String number,
        @NotNull(message = "Informe o ano.") @Min(value = 2000, message = "Ano inválido.")
        @Max(value = 2100, message = "Ano inválido.")
        Integer year,
        @NotBlank(message = "Informe o título.") @Size(max = 200, message = "Máximo de 200 caracteres.")
        String title,
        @NotBlank(message = "Informe a secretaria responsável.") @Size(max = 150, message = "Máximo de 150 caracteres.")
        String department,
        @NotNull(message = "Informe o início das inscrições.") Instant registrationStart,
        @NotNull(message = "Informe o fim das inscrições.") Instant registrationEnd,
        @NotNull(message = "Informe se o processo permite mais de uma inscrição.") Boolean multipleApplicationsAllowed,
        @NotNull(message = "Informe se haverá avaliação de títulos.") Boolean titleEvaluationEnabled) {
}
