package br.gov.pmps.processoseletivo.application.service;

import br.gov.pmps.processoseletivo.domain.model.PasswordResetToken;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import br.gov.pmps.processoseletivo.domain.repository.PasswordResetTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** Gera tokens de senha aleatórios, imprevisíveis e de uso único (ESPECIFICACAO §12). */
@Service
public class PasswordResetTokenIssuer {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final PasswordResetTokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PasswordResetTokenIssuer(
            PasswordResetTokenRepository tokenRepository, ApplicationEventPublisher eventPublisher) {
        this.tokenRepository = tokenRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Deve ser chamado dentro de uma transação; o e-mail só é enviado após o commit. */
    public void issue(UserAccount account, PasswordResetToken.Purpose purpose, Duration validity, Instant now) {
        // Apenas o link mais recente vale.
        tokenRepository.invalidateAll(account.getId(), now);

        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        tokenRepository.save(new PasswordResetToken(account.getId(), hash(rawToken), purpose, now, validity));
        eventPublisher.publishEvent(new PasswordLinkRequested(account.getEmail(), rawToken, purpose));
    }

    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
