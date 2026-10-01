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

/** Registro de mudança de etapa. Somente inserção: o banco nega alteração e exclusão. */
@Entity
@Immutable
@Table(name = "process_stage_history")
public class ProcessStageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_id", nullable = false)
    private UUID processId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_stage")
    private ProcessStage fromStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_stage", nullable = false)
    private ProcessStage toStage;

    @Column(name = "changed_by_account_id", nullable = false)
    private UUID changedByAccountId;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected ProcessStageHistory() {
    }

    public ProcessStageHistory(
            UUID processId, ProcessStage fromStage, ProcessStage toStage, UUID changedByAccountId, Instant changedAt) {
        this.processId = processId;
        this.fromStage = fromStage;
        this.toStage = toStage;
        this.changedByAccountId = changedByAccountId;
        this.changedAt = changedAt;
    }

    public ProcessStage getFromStage() {
        return fromStage;
    }

    public ProcessStage getToStage() {
        return toStage;
    }

    public UUID getChangedByAccountId() {
        return changedByAccountId;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
