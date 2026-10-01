package br.gov.pmps.processoseletivo.application.dto.application;

import br.gov.pmps.processoseletivo.domain.model.Adaptation;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationStatus;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Contratos da área administrativa de inscrições. */
public final class AdminApplicationDtos {

    private AdminApplicationDtos() {
    }

    /** Justificativa: obrigatória no indeferimento e em qualquer alteração de decisão (validado no domínio). */
    public record DecisionRequest(@Size(max = 2000, message = "Máximo de 2000 caracteres.") String reason) {
    }

    /**
     * Dados do candidato registrados na inscrição. {@code hasDisability} e {@code adaptations} são nulos
     * quando o administrador não tem a permissão DADOS_PCD_VISUALIZAR.
     */
    public record CandidateData(
            String fullName,
            String cpf,
            LocalDate birthDate,
            String motherName,
            String email,
            String phone,
            String city,
            String uf,
            Boolean hasDisability,
            Set<Adaptation> adaptations) {
    }

    public record DocumentEntry(
            UUID id, String requirementName, boolean title, String originalName, long sizeBytes, Instant uploadedAt) {
    }

    public record DecisionEntry(
            ApplicationStatus fromStatus, ApplicationStatus toStatus, String reason, String decidedBy, Instant decidedAt) {
    }

    public record AdminApplicationDetail(
            UUID id,
            UUID processId,
            String processNumber,
            String processTitle,
            String positionName,
            ApplicationStatus status,
            String applicationNumber,
            Instant confirmedAt,
            String decisionReason,
            boolean decisionsAllowed,
            CandidateData candidate,
            List<DocumentEntry> documents,
            List<DecisionEntry> decisions) {
    }
}
