package br.gov.pmps.processoseletivo.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Link de redefinição inválido.") @Size(max = 100) String token,
        @NotBlank(message = "Informe a nova senha.") String newPassword) {

    @Override
    public String toString() {
        return "ResetPasswordRequest[***]";
    }
}
