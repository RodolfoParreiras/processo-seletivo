package br.gov.pmps.processoseletivo.presentation.controller;

import br.gov.pmps.processoseletivo.application.dto.process.ReasonRequest;
import br.gov.pmps.processoseletivo.application.usecase.process.ProcessDocumentUseCase;
import br.gov.pmps.processoseletivo.presentation.FileResponses;
import br.gov.pmps.processoseletivo.presentation.MultipartIncomingFile;
import br.gov.pmps.processoseletivo.security.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Documentos do Processo na área administrativa (docs/DECISOES.md). */
@RestController
@RequestMapping("/api/admin/processes/{processId}/documents")
public class AdminProcessDocumentController {

    private final ProcessDocumentUseCase processDocuments;

    public AdminProcessDocumentController(ProcessDocumentUseCase processDocuments) {
        this.processDocuments = processDocuments;
    }

    /** Inclui documentos retirados, com a justificativa. */
    @GetMapping
    @PreAuthorize("hasAuthority('PROCESSO_VISUALIZAR')")
    List<ProcessDocumentUseCase.DocumentView> list(@PathVariable UUID processId) {
        return processDocuments.listAdmin(processId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('RESULTADO_PUBLICAR')")
    Map<String, UUID> publish(
            @PathVariable UUID processId,
            @RequestParam String name,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        return Map.of("id", processDocuments.publish(processId, name, new MultipartIncomingFile(file),
                account.accountId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/{documentId}/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('RESULTADO_PUBLICAR')")
    void withdraw(
            @PathVariable UUID processId,
            @PathVariable UUID documentId,
            @Valid @RequestBody ReasonRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        processDocuments.withdraw(processId, documentId, request.reason(), account.accountId(), httpRequest.getRemoteAddr());
    }

    @GetMapping("/{documentId}/file")
    @PreAuthorize("hasAuthority('PROCESSO_VISUALIZAR')")
    ResponseEntity<InputStreamResource> file(@PathVariable UUID processId, @PathVariable UUID documentId) {
        return FileResponses.attachment(processDocuments.openAdmin(processId, documentId));
    }
}
