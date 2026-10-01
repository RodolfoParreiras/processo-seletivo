package br.gov.pmps.processoseletivo.domain.model;

import br.gov.pmps.processoseletivo.domain.rule.Cpf;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/** Credenciais e estado de acesso. Dados cadastrais ficam em {@link Candidate} ou {@link Administrator}. */
@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(nullable = false, columnDefinition = "bpchar(11)")
    private String cpf;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    // Segredo TOTP cifrado (AES-GCM); nulo enquanto o segundo fator não foi configurado.
    @Column(name = "mfa_secret_encrypted")
    private String mfaSecretEncrypted;

    @Column(name = "mfa_enabled_at")
    private Instant mfaEnabledAt;

    @Column(name = "mfa_last_used_step")
    private Long mfaLastUsedStep;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected UserAccount() {
    }

    public UserAccount(AccountType accountType, Cpf cpf, String email, String passwordHash, Instant now) {
        this.id = UUID.randomUUID();
        this.accountType = accountType;
        this.cpf = cpf.digits();
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.active = true;
        this.passwordChangedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** Após {@code maxAttempts} falhas seguidas, bloqueia a conta e reinicia a contagem. */
    public void registerFailedLogin(Instant now, int maxAttempts, Duration lockDuration) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
            failedLoginAttempts = 0;
        }
        updatedAt = now;
    }

    public void registerSuccessfulLogin(Instant now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
        updatedAt = now;
    }

    public void enableMfa(String encryptedSecret, Instant now) {
        mfaSecretEncrypted = encryptedSecret;
        mfaEnabledAt = now;
        mfaLastUsedStep = null;
        updatedAt = now;
    }

    /** Exige novo cadastro do autenticador no próximo login (ex.: celular perdido). */
    public void resetMfa(Instant now) {
        mfaSecretEncrypted = null;
        mfaEnabledAt = null;
        mfaLastUsedStep = null;
        updatedAt = now;
    }

    public void registerMfaUse(long step) {
        mfaLastUsedStep = step;
    }

    public void deactivate(Instant now) {
        active = false;
        updatedAt = now;
    }

    public void activate(Instant now) {
        active = true;
        updatedAt = now;
    }

    public void changePassword(String newPasswordHash, Instant now) {
        passwordHash = newPasswordHash;
        passwordChangedAt = now;
        failedLoginAttempts = 0;
        lockedUntil = null;
        updatedAt = now;
    }

    public void changeEmail(String newEmail, Instant now) {
        email = normalizeEmail(newEmail);
        updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public boolean isMfaEnabled() {
        return mfaEnabledAt != null;
    }

    public String getMfaSecretEncrypted() {
        return mfaSecretEncrypted;
    }

    public Long getMfaLastUsedStep() {
        return mfaLastUsedStep;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
