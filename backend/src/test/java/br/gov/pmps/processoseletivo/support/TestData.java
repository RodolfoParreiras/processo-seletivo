package br.gov.pmps.processoseletivo.support;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Dados fictícios para testes (AI_RULES §83): CPFs gerados aleatoriamente e e-mails em domínio de teste. */
public final class TestData {

    public static final String VALID_PASSWORD = "Seguro#2024x";

    private TestData() {
    }

    public static String randomCpf() {
        int[] digits = new int[11];
        for (int i = 0; i < 9; i++) {
            digits[i] = ThreadLocalRandom.current().nextInt(10);
        }
        // Evita a sequência de dígitos iguais, que é inválida.
        digits[0] = (digits[1] + 1) % 10;
        digits[9] = checkDigit(digits, 9);
        digits[10] = checkDigit(digits, 10);
        StringBuilder cpf = new StringBuilder();
        for (int digit : digits) {
            cpf.append(digit);
        }
        return cpf.toString();
    }

    public static String randomEmail() {
        return "candidato-" + UUID.randomUUID() + "@example.test";
    }

    public static String registrationJson(String cpf, String email, String password) {
        return """
                {
                  "cpf": "%s",
                  "fullName": "Maria Teste Silva",
                  "birthDate": "1990-03-15",
                  "motherName": "Joana Teste Silva",
                  "email": "%s",
                  "phone": "24999990000",
                  "cep": "25850000",
                  "street": "Rua Fictícia",
                  "addressNumber": "100",
                  "complement": "",
                  "neighborhood": "Centro",
                  "city": "Paraíba do Sul",
                  "uf": "RJ",
                  "password": "%s",
                  "hasDisability": false,
                  "adaptations": []
                }
                """.formatted(cpf, email, password);
    }

    public static String loginJson(String cpf, String password) {
        return """
                {"cpf": "%s", "password": "%s"}
                """.formatted(cpf, password);
    }

    private static int checkDigit(int[] digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += digits[i] * (length + 1 - i);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }
}
