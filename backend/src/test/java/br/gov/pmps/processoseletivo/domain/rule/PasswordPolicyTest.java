package br.gov.pmps.processoseletivo.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

    private static final String NAME = "José da Conceição Souza";
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 3, 15);

    @Test
    void acceptsPasswordMeetingAllRules() {
        assertThat(PasswordPolicy.violations("Seguro#2024x", NAME, BIRTH_DATE)).isEmpty();
    }

    @Test
    void reportsEveryMissingCharacterClass() {
        assertThat(PasswordPolicy.violations("abc", NAME, BIRTH_DATE)).containsExactlyInAnyOrder(
                "A senha deve ter no mínimo 8 caracteres.",
                "A senha deve conter letra maiúscula.",
                "A senha deve conter número.",
                "A senha deve conter caractere especial.");
    }

    @Test
    void rejectsPasswordAboveTechnicalLimit() {
        String longPassword = "Aa1#" + "x".repeat(PasswordPolicy.MAX_LENGTH);

        assertThat(PasswordPolicy.violations(longPassword, NAME, BIRTH_DATE))
                .contains("A senha deve ter no máximo 128 caracteres.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Jose#20241", "x#1CONCEICAOx", "Conceição@99", "souza!A123"})
    void rejectsNameOrSurnameIgnoringCaseAndAccents(String password) {
        assertThat(PasswordPolicy.violations(password, NAME, BIRTH_DATE))
                .contains("A senha não pode conter seu nome ou sobrenome.");
    }

    @Test
    void ignoresShortNameParticles() {
        assertThat(PasswordPolicy.violations("Cadastro#2024", "Ana da Silva", BIRTH_DATE)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Senha#15031990", "Senha#15/03/1990", "Senha#150390", "Senha#1990-03-15"})
    void rejectsFullBirthDateInCommonFormats(String password) {
        assertThat(PasswordPolicy.violations(password, NAME, BIRTH_DATE))
                .contains("A senha não pode conter sua data de nascimento.");
    }

    @Test
    void allowsBirthYearAlone() {
        assertThat(PasswordPolicy.violations("Senha#1990x", NAME, BIRTH_DATE)).isEmpty();
    }

    @Test
    void skipsBirthDateRuleWhenAccountHasNoBirthDate() {
        assertThat(PasswordPolicy.violations("Senha#15031990", NAME, null)).isEmpty();
    }
}
