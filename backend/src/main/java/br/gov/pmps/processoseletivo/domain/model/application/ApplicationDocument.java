package br.gov.pmps.processoseletivo.domain.model.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Arquivo enviado pelo candidato para um documento exigido. */
@Entity
@Table(name = "application_document")
public class ApplicationDocument {

    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false, updatable = false)
    private UUID applicationId;

    @Column(name = "requirement_id", nullable = false, updatable = false)
    private UUID requirementId;

    @Column(name = "file_id", nullable = false, updatable = false)
    private UUID fileId;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    protected ApplicationDocument() {
    }

    public ApplicationDocument(UUID applicationId, UUID requirementId, UUID fileId, Instant now) {
        this.id = UUID.randomUUID();
        this.applicationId = applicationId;
        this.requirementId = requirementId;
        this.fileId = fileId;
        this.uploadedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getRequirementId() {
        return requirementId;
    }

    public UUID getFileId() {
        return fileId;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
