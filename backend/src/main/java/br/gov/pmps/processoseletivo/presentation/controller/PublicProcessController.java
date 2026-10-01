package br.gov.pmps.processoseletivo.presentation.controller;

import br.gov.pmps.processoseletivo.application.dto.PageResponse;
import br.gov.pmps.processoseletivo.application.dto.process.ProcessDetailResponse;
import br.gov.pmps.processoseletivo.application.dto.process.ProcessSummaryResponse;
import br.gov.pmps.processoseletivo.application.usecase.process.ProcessDocumentUseCase;
import br.gov.pmps.processoseletivo.application.usecase.process.ProcessQueryUseCase;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessStatus;
import br.gov.pmps.processoseletivo.presentation.FileResponses;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta pública de processos e editais (docs/DECISOES.md: lista, edital e resultados podem ser públicos). */
@RestController
@RequestMapping("/api/processes")
public class PublicProcessController {

    private final ProcessQueryUseCase processQuery;
    private final ProcessDocumentUseCase processDocuments;

    public PublicProcessController(ProcessQueryUseCase processQuery, ProcessDocumentUseCase processDocuments) {
        this.processQuery = processQuery;
        this.processDocuments = processDocuments;
    }

    @GetMapping
    PageResponse<ProcessSummaryResponse> list(
            @RequestParam(required = false) ProcessStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return processQuery.listPublic(status, page, size);
    }

    @GetMapping("/{processId}")
    ProcessDetailResponse get(@PathVariable UUID processId) {
        return processQuery.getPublic(processId);
    }

    @GetMapping("/{processId}/notices/{version}/file")
    ResponseEntity<InputStreamResource> noticeFile(@PathVariable UUID processId, @PathVariable int version) {
        return FileResponses.attachment(processQuery.openPublicNotice(processId, version));
    }

    @GetMapping("/{processId}/documents")
    List<ProcessDocumentUseCase.DocumentView> documents(@PathVariable UUID processId) {
        return processDocuments.listPublic(processId);
    }

    @GetMapping("/{processId}/documents/{documentId}/file")
    ResponseEntity<InputStreamResource> documentFile(@PathVariable UUID processId, @PathVariable UUID documentId) {
        return FileResponses.attachment(processDocuments.openPublic(processId, documentId));
    }
}
