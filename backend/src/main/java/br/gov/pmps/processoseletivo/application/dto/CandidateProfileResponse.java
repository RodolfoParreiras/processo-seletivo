package br.gov.pmps.processoseletivo.application.dto;

import br.gov.pmps.processoseletivo.domain.model.Adaptation;
import java.time.LocalDate;
import java.util.Set;

/** Dados do próprio candidato; nunca é usado para expor dados de terceiros. */
public record CandidateProfileResponse(
        String cpf,
        String fullName,
        LocalDate birthDate,
        String motherName,
        String email,
        String phone,
        String cep,
        String street,
        String addressNumber,
        String complement,
        String neighborhood,
        String city,
        String uf,
        boolean hasDisability,
        Set<Adaptation> adaptations) {
}
