package br.gov.pmps.processoseletivo.domain.rule;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Política de senha definida pela Prefeitura (docs/DECISOES.md).
 * Devolve todas as regras violadas para que o usuário corrija de uma vez.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    // Limite técnico: evita custo excessivo do hash com entradas enormes.
    public static final int MAX_LENGTH = 128;

    private static final int MIN_NAME_PART_LENGTH = 3;
    private static final Set<String> NAME_PARTICLES = Set.of("da", "de", "do", "das", "dos", "e");
    private static final List<DateTimeFormatter> BIRTH_DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("ddMMyyyy"),
            DateTimeFormatter.ofPattern("ddMMyy"),
            DateTimeFormatter.ofPattern("yyyyMMdd"));

    private PasswordPolicy() {
    }

    /**
     * @param fullName  nome completo do titular da conta
     * @param birthDate data de nascimento, ou {@code null} quando a conta não possui esse dado
     */
    public static List<String> violations(String password, String fullName, LocalDate birthDate) {
        List<String> violations = new ArrayList<>();
        if (password == null || password.length() < MIN_LENGTH) {
            violations.add("A senha deve ter no mínimo " + MIN_LENGTH + " caracteres.");
        }
        if (password == null) {
            return violations;
        }
        if (password.length() > MAX_LENGTH) {
            violations.add("A senha deve ter no máximo " + MAX_LENGTH + " caracteres.");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            violations.add("A senha deve conter letra maiúscula.");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            violations.add("A senha deve conter letra minúscula.");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            violations.add("A senha deve conter número.");
        }
        if (password.chars().noneMatch(PasswordPolicy::isSpecial)) {
            violations.add("A senha deve conter caractere especial.");
        }
        if (containsNamePart(password, fullName)) {
            violations.add("A senha não pode conter seu nome ou sobrenome.");
        }
        if (containsBirthDate(password, birthDate)) {
            violations.add("A senha não pode conter sua data de nascimento.");
        }
        return violations;
    }

    private static boolean isSpecial(int character) {
        return !Character.isLetterOrDigit(character) && !Character.isWhitespace(character);
    }

    private static boolean containsNamePart(String password, String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return false;
        }
        String normalizedPassword = normalize(password);
        for (String part : normalize(fullName).split("[\\s\\-']+")) {
            if (part.length() >= MIN_NAME_PART_LENGTH
                    && !NAME_PARTICLES.contains(part)
                    && normalizedPassword.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsBirthDate(String password, LocalDate birthDate) {
        if (birthDate == null) {
            return false;
        }
        // Remove separadores para identificar a data escrita como 15/03/1990, 15-03-1990 etc.
        String passwordDigits = password.replaceAll("\\D", "");
        return BIRTH_DATE_FORMATS.stream()
                .map(birthDate::format)
                .anyMatch(passwordDigits::contains);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
