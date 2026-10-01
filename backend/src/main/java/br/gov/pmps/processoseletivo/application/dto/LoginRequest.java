package br.gov.pmps.processoseletivo.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Sem validação de formato do CPF: um CPF inválido recebe a mesma resposta genérica de credencial inválida. */
public record LoginRequest(
        @NotBlank(message = "Informe o CPF.") @Size(max = 14) String cpf,
        @NotBlank(message = "Informe a senha.") @Size(max = 256) String password) {

    @Override
    public String toString() {
        return "LoginRequest[***]";
    }
}
