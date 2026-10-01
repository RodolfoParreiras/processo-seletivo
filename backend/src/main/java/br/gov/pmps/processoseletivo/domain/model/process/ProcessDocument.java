package br.gov.pmps.processoseletivo.domain.model.process;

import br.gov.pmps.processoseletivo.shared.error.DomainRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Documento publicado no processo, com nome definido pelo administrador (docs/DECISOES.md).
 * Não é excluído: pode ser retirado da página pública com justificativa.
 */
@Entity
@Table(name = "process_document")
public class ProcessDocument {

    @Id
    private UUID id;

    @Column(name = "process_id", nullable = false, updatable = false)
    private UUID processId;

    @Column(nullable = false, updatable = false)
    private String name;

    @Column(name = "file_id", nullable = false, updatable = false)
    private UUID fileId;

    @Column(name = "published_at", nullable = false, updatable = false)
    private Instant publishedAt;

    @Column(name = "published_by_account_id", nullable = false, updatable = false)
    private UUID publishedByAccountId;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Column(name = "withdrawn_by_account_id")
    private UUID withdrawnByAccountId;

    @Column(name = "withdrawal_reason")
    private String withdrawalReason;

    protected ProcessDocument() {
    }

    public ProcessDocument(UUID processId, String name, UUID fileId, UUID accountId, Instant now) {
        this.id = UUID.randomUUID();
        this.processId = processId;
        this.name = name.trim();
        this.fileId = fileId;
        this.publishedByAccountId = accountId;
        this.publishedAt = now;
    }

    public void withdraw(String reason, UUID accountId, Instant now) {
        if (isWithdrawn()) {
            throw new DomainRuleException("O documento já foi retirado.");
        }
        if (reason == null || reason.isBlank()) {
            throw new DomainRuleException("Informe a justificativa da retirada.");
        }
        this.withdrawalReason = reason.trim();
        this.withdrawnByAccountId = accountId;
        this.withdrawnAt = now;
    }

    public boolean isWithdrawn() {
        return withdrawnAt != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProcessId() {
        return processId;
    }

    public String getName() {
        return name;
    }

    public UUID getFileId() {
        return fileId;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    public String getWithdrawalReason() {
        return withdrawalReason;
    }
}
