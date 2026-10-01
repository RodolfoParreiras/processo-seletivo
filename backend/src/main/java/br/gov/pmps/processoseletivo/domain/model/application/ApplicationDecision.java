package br.gov.pmps.processoseletivo.domain.model.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Histórico de deferimentos/indeferimentos. Somente inserção: o banco nega alteração e exclusão. */
@Entity
@Immutable
@Table(name = "application_decision")
public class ApplicationDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private ApplicationStatus toStatus;

    @Column
    private String reason;

    @Column(name = "decided_by_account_id", nullable = false)
    private UUID decidedByAccountId;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    protected ApplicationDecision() {
    }

    public ApplicationDecision(
            UUID applicationId, ApplicationStatus fromStatus, ApplicationStatus toStatus, String reason,
            UUID decidedByAccountId, Instant decidedAt) {
        this.applicationId = applicationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason == null || reason.isBlank() ? null : reason.trim();
        this.decidedByAccountId = decidedByAccountId;
        this.decidedAt = decidedAt;
    }

    public ApplicationStatus getFromStatus() {
        return fromStatus;
    }

    public ApplicationStatus getToStatus() {
        return toStatus;
    }

    public String getReason() {
        return reason;
    }

    public UUID getDecidedByAccountId() {
        return decidedByAccountId;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
