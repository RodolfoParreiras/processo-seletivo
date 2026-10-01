package br.gov.pmps.processoseletivo.application.dto.application;

import br.gov.pmps.processoseletivo.domain.model.application.ApplicationDocument;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Contratos da API de inscrições do candidato. */
public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    public record StartApplicationRequest(
            @NotNull(message = "Informe o processo.") UUID processId,
            @NotNull(message = "Informe o cargo.") UUID positionId) {
    }

    public record ApplicationSummary(
            UUID id,
            UUID processId,
            String processNumber,
            String processTitle,
            String positionName,
            ApplicationStatus status,
            String applicationNumber,
            Instant createdAt,
            Instant confirmedAt) {
    }

    public record DocumentItem(
            UUID id, String originalName, long sizeBytes, Instant uploadedAt, ApplicationDocument.Status status) {
    }

    public record RequirementItem(
            UUID id, String name, String description, boolean mandatory, boolean title, List<DocumentItem> documents) {
    }

    public record ApplicationDetail(
            UUID id,
            UUID processId,
            String processNumber,
            String processTitle,
            String positionName,
            ApplicationStatus status,
            String applicationNumber,
            String verificationCode,
            Instant createdAt,
            Instant confirmedAt,
            Instant registrationEnd,
            boolean acceptingApplications,
            int maxDocuments,
            List<RequirementItem> requirements) {
    }

    /** Verificação pública do comprovante: nenhum dado pessoal do candidato. */
    public record ReceiptVerification(
            String applicationNumber,
            String processNumber,
            String processTitle,
            String positionName,
            Instant confirmedAt) {
    }
}
