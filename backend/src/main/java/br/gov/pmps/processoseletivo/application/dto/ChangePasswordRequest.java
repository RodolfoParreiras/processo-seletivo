package br.gov.pmps.processoseletivo.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Informe a senha atual.") @Size(max = 256) String currentPassword,
        @NotBlank(message = "Informe a nova senha.") String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[***]";
    }
}
