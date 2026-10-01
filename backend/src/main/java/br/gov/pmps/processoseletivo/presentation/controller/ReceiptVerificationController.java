package br.gov.pmps.processoseletivo.presentation.controller;

import br.gov.pmps.processoseletivo.application.dto.application.ApplicationDtos.ReceiptVerification;
import br.gov.pmps.processoseletivo.application.usecase.application.ApplicationQueryUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Verificação pública de autenticidade do comprovante. Não devolve dados pessoais. */
@RestController
@RequestMapping("/api/receipts")
public class ReceiptVerificationController {

    private final ApplicationQueryUseCase query;

    public ReceiptVerificationController(ApplicationQueryUseCase query) {
        this.query = query;
    }

    @GetMapping("/{code}")
    ReceiptVerification verify(@PathVariable String code) {
        return query.verify(code);
    }
}
