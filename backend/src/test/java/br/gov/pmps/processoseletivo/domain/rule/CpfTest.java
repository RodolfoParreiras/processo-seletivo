package br.gov.pmps.processoseletivo.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gov.pmps.processoseletivo.support.TestData;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CpfTest {

    @RepeatedTest(20)
    void acceptsGeneratedValidCpf() {
        assertThat(Cpf.isValid(TestData.randomCpf())).isTrue();
    }

    @Test
    void acceptsFormattedCpfAndStoresOnlyDigits() {
        String digits = TestData.randomCpf();
        String formatted = digits.substring(0, 3) + "." + digits.substring(3, 6) + "."
                + digits.substring(6, 9) + "-" + digits.substring(9);

        assertThat(Cpf.of(formatted).digits()).isEqualTo(digits);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"11111111111", "00000000000", "1234567890", "123456789012", "abcdefghijk"})
    void rejectsInvalidFormats(String value) {
        assertThat(Cpf.isValid(value)).isFalse();
    }

    @Test
    void rejectsWrongCheckDigits() {
        String digits = TestData.randomCpf();
        char wrongLastDigit = (char) ('0' + ((digits.charAt(10) - '0' + 1) % 10));

        assertThat(Cpf.isValid(digits.substring(0, 10) + wrongLastDigit)).isFalse();
        assertThatThrownBy(() -> Cpf.of(digits.substring(0, 10) + wrongLastDigit))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void masksAllButLastTwoDigits() {
        String digits = TestData.randomCpf();

        assertThat(Cpf.mask(digits)).isEqualTo("***.***.***-" + digits.substring(9));
        assertThat(Cpf.of(digits).toString()).doesNotContain(digits.substring(0, 9));
    }
}
