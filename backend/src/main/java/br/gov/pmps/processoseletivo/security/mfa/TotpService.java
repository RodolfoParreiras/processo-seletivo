package br.gov.pmps.processoseletivo.security.mfa;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.OptionalLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Códigos TOTP (RFC 6238, HMAC-SHA1, 6 dígitos, 30 s), compatíveis com aplicativos autenticadores.
 * Usa apenas o HMAC da plataforma Java; nenhum algoritmo criptográfico próprio.
 */
@Component
public class TotpService {

    static final int SECRET_BYTES = 20;
    static final long STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    // Tolera um passo de diferença de relógio entre servidor e celular.
    private static final int ALLOWED_DRIFT_STEPS = 1;

    private final SecureRandom secureRandom = new SecureRandom();

    public String newSecret() {
        byte[] secret = new byte[SECRET_BYTES];
        secureRandom.nextBytes(secret);
        return Base32.encode(secret);
    }

    /** URI lida pelo aplicativo autenticador (formato otpauth://totp). */
    public String provisioningUri(String base32Secret, String accountLabel) {
        String issuer = "Processos Seletivos PMPS";
        return "otpauth://totp/" + encode(issuer + ":" + accountLabel)
                + "?secret=" + base32Secret + "&issuer=" + encode(issuer)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + STEP_SECONDS;
    }

    /**
     * @param lastUsedStep último passo aceito para a conta; códigos de passos anteriores ou iguais são recusados
     * @return passo de tempo correspondente ao código, quando válido
     */
    public OptionalLong verify(String base32Secret, String code, Instant now, Long lastUsedStep) {
        if (code == null || !code.matches("\\d{" + DIGITS + "}")) {
            return OptionalLong.empty();
        }
        byte[] key = Base32.decode(base32Secret);
        long currentStep = now.getEpochSecond() / STEP_SECONDS;
        for (long step = currentStep - ALLOWED_DRIFT_STEPS; step <= currentStep + ALLOWED_DRIFT_STEPS; step++) {
            if (lastUsedStep != null && step <= lastUsedStep) {
                continue;
            }
            if (MessageDigest.isEqual(codeAt(key, step).getBytes(StandardCharsets.US_ASCII),
                    code.getBytes(StandardCharsets.US_ASCII))) {
                return OptionalLong.of(step);
            }
        }
        return OptionalLong.empty();
    }

    /** Código válido no instante informado (usado também por testes automatizados). */
    public String codeAt(String base32Secret, Instant instant) {
        return codeAt(Base32.decode(base32Secret), instant.getEpochSecond() / STEP_SECONDS);
    }

    static String codeAt(byte[] key, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA1 indisponível", exception);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
