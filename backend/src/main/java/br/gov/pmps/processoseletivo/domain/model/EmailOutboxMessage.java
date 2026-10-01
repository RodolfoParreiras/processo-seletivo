package br.gov.pmps.processoseletivo.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * E-mail aguardando envio. Gravado na mesma transação da operação de negócio; o envio ocorre depois,
 * com novas tentativas em caso de falha (ESPECIFICACAO §28). Não deve conter senhas ou tokens.
 */
@Entity
@Table(name = "email_outbox")
public class EmailOutboxMessage {

    public enum Status {
        PENDING,
        SENT,
        FAILED
    }

    public static final int MAX_ATTEMPTS = 5;

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String recipient;

    @Column(nullable = false, updatable = false)
    private String subject;

    @Column(nullable = false, updatable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected EmailOutboxMessage() {
    }

    public EmailOutboxMessage(String recipient, String subject, String body, Instant now) {
        this.id = UUID.randomUUID();
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.status = Status.PENDING;
        this.nextAttemptAt = now;
        this.createdAt = now;
    }

    public void markSent(Instant now) {
        attempts++;
        status = Status.SENT;
        sentAt = now;
        lastError = null;
    }

    /** Espera cresce a cada tentativa; após o limite, fica como FAILED para tratamento manual. */
    public void markFailed(String error, Instant now) {
        attempts++;
        lastError = error == null ? null : error.substring(0, Math.min(error.length(), 200));
        if (attempts >= MAX_ATTEMPTS) {
            status = Status.FAILED;
        } else {
            nextAttemptAt = now.plus(Duration.ofMinutes(5L * attempts));
        }
    }

    public UUID getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public Status getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }
}
