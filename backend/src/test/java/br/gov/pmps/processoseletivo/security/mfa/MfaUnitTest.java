package br.gov.pmps.processoseletivo.security.mfa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class MfaUnitTest {

    // Segredo do vetor de teste da RFC 6238 ("12345678901234567890" em ASCII), em Base32.
    private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    private final TotpService totp = new TotpService();

    @Test
    void matchesRfc6238TestVectors() {
        // RFC 6238, apêndice B (SHA1), últimos 6 dígitos de 94287082, 07081804 e 89005924.
        assertThat(totp.codeAt(RFC_SECRET, Instant.ofEpochSecond(59))).isEqualTo("287082");
        assertThat(totp.codeAt(RFC_SECRET, Instant.ofEpochSecond(1111111109))).isEqualTo("081804");
        assertThat(totp.codeAt(RFC_SECRET, Instant.ofEpochSecond(1234567890))).isEqualTo("005924");
    }

    @Test
    void acceptsAdjacentStepAndRejectsReuse() {
        Instant now = Instant.ofEpochSecond(1_800_000_000L);
        String code = totp.codeAt(RFC_SECRET, now);

        var step = totp.verify(RFC_SECRET, code, now.plusSeconds(30), null);
        assertThat(step).isPresent();
        assertThat(totp.verify(RFC_SECRET, code, now, step.getAsLong())).isEmpty();
    }

    @Test
    void rejectsMalformedAndOldCodes() {
        Instant now = Instant.ofEpochSecond(1_800_000_000L);

        assertThat(totp.verify(RFC_SECRET, "12345", now, null)).isEmpty();
        assertThat(totp.verify(RFC_SECRET, "abcdef", now, null)).isEmpty();
        assertThat(totp.verify(RFC_SECRET, totp.codeAt(RFC_SECRET, now.minusSeconds(120)), now, null)).isEmpty();
    }

    @Test
    void newSecretsAreRandomBase32() {
        assertThat(totp.newSecret()).matches("[A-Z2-7]{32}").isNotEqualTo(totp.newSecret());
    }

    @Test
    void cipherRoundTripsAndRequires256BitKey() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        MfaSecretCipher cipher = new MfaSecretCipher(key);

        String encrypted = cipher.encrypt(RFC_SECRET);
        assertThat(encrypted).doesNotContain(RFC_SECRET).isNotEqualTo(cipher.encrypt(RFC_SECRET));
        assertThat(cipher.decrypt(encrypted)).isEqualTo(RFC_SECRET);
        assertThatThrownBy(() -> new MfaSecretCipher(Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class);
    }
}
