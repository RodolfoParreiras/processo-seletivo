package br.gov.pmps.processoseletivo.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gov.pmps.processoseletivo.domain.rule.Cpf;
import br.gov.pmps.processoseletivo.support.TestData;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DomainModelTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");

    @Test
    void accountIsLockedAfterMaxFailedAttemptsAndUnlocksAfterDuration() {
        UserAccount account = newAccount();

        for (int attempt = 0; attempt < 4; attempt++) {
            account.registerFailedLogin(NOW, 5, Duration.ofMinutes(15));
        }
        assertThat(account.isLocked(NOW)).isFalse();

        account.registerFailedLogin(NOW, 5, Duration.ofMinutes(15));
        assertThat(account.isLocked(NOW)).isTrue();
        assertThat(account.isLocked(NOW.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void successfulLoginResetsFailedAttempts() {
        UserAccount account = newAccount();
        account.registerFailedLogin(NOW, 5, Duration.ofMinutes(15));

        account.registerSuccessfulLogin(NOW);

        assertThat(account.getFailedLoginAttempts()).isZero();
    }

    @Test
    void emailIsNormalized() {
        assertThat(UserAccount.normalizeEmail("  Maria@Example.TEST ")).isEqualTo("maria@example.test");
    }

    @Test
    void personWithDisabilityMustInformAdaptation() {
        assertThatThrownBy(() -> Candidate.validateAdaptations(true, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> Candidate.validateAdaptations(true, Set.of(Adaptation.NONE)))
                .doesNotThrowAnyException();
        assertThatCode(() -> Candidate.validateAdaptations(
                true, Set.of(Adaptation.LIBRAS_INTERPRETER, Adaptation.ENLARGED_TEST)))
                .doesNotThrowAnyException();
    }

    @Test
    void noneCannotBeCombinedWithOtherAdaptations() {
        assertThatThrownBy(() -> Candidate.validateAdaptations(
                true, Set.of(Adaptation.NONE, Adaptation.READING_ASSISTANCE)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void personWithoutDisabilityCannotInformAdaptations() {
        assertThatThrownBy(() -> Candidate.validateAdaptations(false, Set.of(Adaptation.NONE)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> Candidate.validateAdaptations(false, Set.of())).doesNotThrowAnyException();
    }

    @Test
    void passwordResetTokenIsSingleUseAndExpires() {
        PasswordResetToken token = new PasswordResetToken(
                newAccount().getId(), "a".repeat(64), PasswordResetToken.Purpose.PASSWORD_RESET,
                NOW, Duration.ofMinutes(30));

        assertThat(token.isUsable(NOW.plus(Duration.ofMinutes(29)))).isTrue();
        assertThat(token.isUsable(NOW.plus(Duration.ofMinutes(30)))).isFalse();

        token.markUsed(NOW);
        assertThat(token.isUsable(NOW)).isFalse();
    }

    @Test
    void outboxMessageRetriesWithBackoffAndFailsAfterLimit() {
        EmailOutboxMessage message = new EmailOutboxMessage("x@example.test", "Assunto", "Corpo", NOW);

        for (int attempt = 1; attempt < EmailOutboxMessage.MAX_ATTEMPTS; attempt++) {
            message.markFailed("MailSendException", NOW);
            assertThat(message.getStatus()).isEqualTo(EmailOutboxMessage.Status.PENDING);
        }
        message.markFailed("MailSendException", NOW);

        assertThat(message.getStatus()).isEqualTo(EmailOutboxMessage.Status.FAILED);
        assertThat(message.getAttempts()).isEqualTo(EmailOutboxMessage.MAX_ATTEMPTS);
    }

    private static UserAccount newAccount() {
        return new UserAccount(AccountType.CANDIDATE, Cpf.of(TestData.randomCpf()), TestData.randomEmail(), "hash", NOW);
    }
}
