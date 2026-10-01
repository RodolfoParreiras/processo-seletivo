package br.gov.pmps.processoseletivo.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeEmailRequest(
        @NotBlank(message = "Informe o novo e-mail.") @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "Máximo de 254 caracteres.")
        String newEmail,
        @NotBlank(message = "Informe a senha atual.") @Size(max = 256) String currentPassword) {

    @Override
    public String toString() {
        return "ChangeEmailRequest[***]";
    }
}
