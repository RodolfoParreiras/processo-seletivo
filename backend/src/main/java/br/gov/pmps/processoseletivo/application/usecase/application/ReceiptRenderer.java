package br.gov.pmps.processoseletivo.application.usecase.application;

import java.time.Instant;
import java.util.List;

/** Gera o comprovante de inscrição (ESPECIFICACAO §27). A implementação de PDF fica na infraestrutura. */
public interface ReceiptRenderer {

    record ReceiptData(
            String processNumber,
            String processTitle,
            String department,
            String applicationNumber,
            String positionName,
            Instant confirmedAt,
            String candidateName,
            String maskedCpf,
            boolean hasDisability,
            List<String> adaptations,
            String declaration,
            String verificationCode,
            String verificationUrl) {
    }

    byte[] render(ReceiptData data);
}
