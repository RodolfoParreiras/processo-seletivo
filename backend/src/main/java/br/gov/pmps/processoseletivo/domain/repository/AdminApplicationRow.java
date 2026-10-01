package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.application.ApplicationStatus;
import java.time.Instant;
import java.util.UUID;

/** Linha da lista administrativa de inscrições, com dados do snapshot (sem dados sensíveis). */
public record AdminApplicationRow(
        UUID id,
        String applicationNumber,
        String candidateName,
        String positionName,
        ApplicationStatus status,
        Instant confirmedAt) {
}
