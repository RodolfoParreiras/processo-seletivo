package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.application.ApplicationStatus;
import java.time.Instant;
import java.util.UUID;

/** Projeção para "Minhas Candidaturas": inscrição com processo e cargo, carregada em uma consulta. */
public record CandidateApplicationRow(
        UUID id,
        UUID processId,
        String processNumber,
        int processYear,
        String processTitle,
        String positionName,
        ApplicationStatus status,
        String applicationNumber,
        Instant createdAt,
        Instant confirmedAt) {
}
