package br.gov.pmps.processoseletivo.domain.model.process;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Versão do edital (ESPECIFICACAO §15). Rascunho pode ter o arquivo trocado; versão publicada
 * nunca muda: uma retificação cria nova versão e marca a anterior como substituída.
 */
@Entity
@Table(name = "process_notice")
public class ProcessNotice {

    public enum Status {
        DRAFT,
        CURRENT,
        SUPERSEDED
    }

    @Id
    private UUID id;

    @Column(name = "process_id", nullable = false, updatable = false)
    private UUID processId;

    @Column(name = "notice_version", nullable = false, updatable = false)
    private int noticeVersion;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "change_reason", updatable = false)
    private String changeReason;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by_account_id")
    private UUID publishedByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by_account_id", nullable = false, updatable = false)
    private UUID createdByAccountId;

    protected ProcessNotice() {
    }

    private ProcessNotice(
            UUID processId, int noticeVersion, UUID fileId, Status status, String changeReason,
            UUID accountId, Instant now) {
        this.id = UUID.randomUUID();
        this.processId = processId;
        this.noticeVersion = noticeVersion;
        this.fileId = fileId;
        this.status = status;
        this.changeReason = changeReason;
        this.createdAt = now;
        this.createdByAccountId = accountId;
        if (status == Status.CURRENT) {
            this.publishedAt = now;
            this.publishedByAccountId = accountId;
        }
    }

    public static ProcessNotice draft(UUID processId, UUID fileId, UUID accountId, Instant now) {
        return new ProcessNotice(processId, 1, fileId, Status.DRAFT, null, accountId, now);
    }

    public static ProcessNotice rectification(
            UUID processId, int noticeVersion, UUID fileId, String reason, UUID accountId, Instant now) {
        return new ProcessNotice(processId, noticeVersion, fileId, Status.CURRENT, reason.trim(), accountId, now);
    }

    /** @return arquivo anterior, que deixa de ser referenciado */
    public UUID replaceDraftFile(UUID newFileId) {
        if (status != Status.DRAFT) {
            throw new IllegalStateException("Somente edital em rascunho pode ter o arquivo trocado");
        }
        UUID previousFileId = fileId;
        fileId = newFileId;
        return previousFileId;
    }

    public void publish(UUID accountId, Instant now) {
        if (status != Status.DRAFT) {
            throw new IllegalStateException("Edital já publicado");
        }
        status = Status.CURRENT;
        publishedAt = now;
        publishedByAccountId = accountId;
    }

    public void supersede() {
        if (status != Status.CURRENT) {
            throw new IllegalStateException("Somente a versão vigente pode ser substituída");
        }
        status = Status.SUPERSEDED;
    }

    public boolean isPublished() {
        return status != Status.DRAFT;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProcessId() {
        return processId;
    }

    public int getNoticeVersion() {
        return noticeVersion;
    }

    public UUID getFileId() {
        return fileId;
    }

    public Status getStatus() {
        return status;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
