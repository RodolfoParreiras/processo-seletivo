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

/** Token de uso único para definir ou redefinir senha. Guarda apenas o hash do valor enviado por e-mail. */
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    public enum Purpose {
        PASSWORD_RESET,
        PASSWORD_SETUP
    }

    @Id
    private UUID id;

    @Column(name = "user_account_id", nullable = false, updatable = false)
    private UUID userAccountId;

    @Column(name = "token_hash", nullable = false, updatable = false, columnDefinition = "bpchar(64)")
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Purpose purpose;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(UUID userAccountId, String tokenHash, Purpose purpose, Instant now, Duration validity) {
        this.id = UUID.randomUUID();
        this.userAccountId = userAccountId;
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.createdAt = now;
        this.expiresAt = now.plus(validity);
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed(Instant now) {
        usedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserAccountId() {
        return userAccountId;
    }

    public Purpose getPurpose() {
        return purpose;
    }
}
