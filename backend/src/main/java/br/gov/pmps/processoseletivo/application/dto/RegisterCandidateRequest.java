package br.gov.pmps.processoseletivo.application.dto;

import br.gov.pmps.processoseletivo.domain.model.Adaptation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

/** Campos do cadastro (ESPECIFICACAO §7). Valores numéricos chegam sem máscara. */
public record RegisterCandidateRequest(
        @NotBlank(message = "Informe o CPF.") @ValidCpf String cpf,
        @NotBlank(message = "Informe o nome completo.") @Size(max = 150, message = "Máximo de 150 caracteres.")
        String fullName,
        @NotNull(message = "Informe a data de nascimento.") @Past(message = "Data de nascimento inválida.")
        LocalDate birthDate,
        @NotBlank(message = "Informe o nome da mãe.") @Size(max = 150, message = "Máximo de 150 caracteres.")
        String motherName,
        @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "Máximo de 254 caracteres.")
        String email,
        @NotBlank(message = "Informe o telefone.") @Pattern(regexp = "\\d{10,11}", message = "Telefone inválido.")
        String phone,
        @NotBlank(message = "Informe o CEP.") @Pattern(regexp = "\\d{8}", message = "CEP inválido.")
        String cep,
        @NotBlank(message = "Informe o endereço.") @Size(max = 150, message = "Máximo de 150 caracteres.")
        String street,
        @NotBlank(message = "Informe o número.") @Size(max = 10, message = "Máximo de 10 caracteres.")
        String addressNumber,
        @Size(max = 60, message = "Máximo de 60 caracteres.")
        String complement,
        @NotBlank(message = "Informe o bairro.") @Size(max = 80, message = "Máximo de 80 caracteres.")
        String neighborhood,
        @NotBlank(message = "Informe a cidade.") @Size(max = 80, message = "Máximo de 80 caracteres.")
        String city,
        @NotBlank(message = "Informe a UF.")
        @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
                message = "UF inválida.")
        String uf,
        @NotBlank(message = "Informe a senha.") String password,
        @NotNull(message = "Informe se é pessoa com deficiência.") Boolean hasDisability,
        Set<Adaptation> adaptations) {

    @Override
    public String toString() {
        // Evita que dados pessoais ou a senha apareçam em logs por acidente.
        return "RegisterCandidateRequest[***]";
    }
}
