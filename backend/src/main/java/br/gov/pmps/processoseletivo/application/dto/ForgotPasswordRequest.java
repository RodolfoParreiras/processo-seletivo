package br.gov.pmps.processoseletivo.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") @Size(max = 254) String email) {

    @Override
    public String toString() {
        return "ForgotPasswordRequest[***]";
    }
}
