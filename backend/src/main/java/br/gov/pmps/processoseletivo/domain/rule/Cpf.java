package br.gov.pmps.processoseletivo.domain.rule;

/** CPF validado pelos dígitos verificadores. Armazena somente os 11 dígitos, sem máscara. */
public final class Cpf {

    private final String digits;

    private Cpf(String digits) {
        this.digits = digits;
    }

    public static Cpf of(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException("CPF inválido");
        }
        return new Cpf(onlyDigits(value));
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        String digits = onlyDigits(value);
        if (digits.length() != 11 || digits.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(digits, 9) == digits.charAt(9) - '0'
                && checkDigit(digits, 10) == digits.charAt(10) - '0';
    }

    /** Formato para logs e auditoria, que não precisam do CPF completo (ESPECIFICACAO §67). */
    public static String mask(String value) {
        String digits = value == null ? "" : onlyDigits(value);
        if (digits.length() != 11) {
            return "***.***.***-**";
        }
        return "***.***.***-" + digits.substring(9);
    }

    public String digits() {
        return digits;
    }

    public String masked() {
        return mask(digits);
    }

    private static int checkDigit(String digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (length + 1 - i);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }

    private static String onlyDigits(String value) {
        return value.replaceAll("\\D", "");
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Cpf cpf && cpf.digits.equals(digits);
    }

    @Override
    public int hashCode() {
        return digits.hashCode();
    }

    @Override
    public String toString() {
        return masked();
    }
}
