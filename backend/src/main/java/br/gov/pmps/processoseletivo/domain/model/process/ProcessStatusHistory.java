package br.gov.pmps.processoseletivo.domain.model.process;

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

/** Registro de mudança de situação. Somente inserção: o banco nega alteração e exclusão. */
@Entity
@Immutable
@Table(name = "process_status_history")
public class ProcessStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_id", nullable = false)
    private UUID processId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ProcessStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private ProcessStatus toStatus;

    @Column
    private String reason;

    @Column(name = "changed_by_account_id")
    private UUID changedByAccountId;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected ProcessStatusHistory() {
    }

    /** @param changedByAccountId {@code null} para transições automáticas pelas datas */
    public ProcessStatusHistory(
            UUID processId, StatusChange change, String reason, UUID changedByAccountId, Instant changedAt) {
        this.processId = processId;
        this.fromStatus = change.from();
        this.toStatus = change.to();
        this.reason = reason == null || reason.isBlank() ? null : reason.trim();
        this.changedByAccountId = changedByAccountId;
        this.changedAt = changedAt;
    }

    public ProcessStatus getFromStatus() {
        return fromStatus;
    }

    public ProcessStatus getToStatus() {
        return toStatus;
    }

    public String getReason() {
        return reason;
    }

    public UUID getChangedByAccountId() {
        return changedByAccountId;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
