package br.gov.pmps.processoseletivo.presentation.controller;

import br.gov.pmps.processoseletivo.application.dto.application.ApplicationDtos.ApplicationDetail;
import br.gov.pmps.processoseletivo.application.dto.application.ApplicationDtos.ApplicationSummary;
import br.gov.pmps.processoseletivo.application.dto.application.ApplicationDtos.StartApplicationRequest;
import br.gov.pmps.processoseletivo.application.usecase.application.ApplicationDraftUseCase;
import br.gov.pmps.processoseletivo.application.usecase.application.ApplicationQueryUseCase;
import br.gov.pmps.processoseletivo.application.usecase.application.ConfirmApplicationUseCase;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/** Inscrições do candidato autenticado. Toda consulta é restrita às inscrições do titular da sessão. */
@RestController
@RequestMapping("/api/candidate/applications")
public class CandidateApplicationController {

    private final ApplicationDraftUseCase drafts;
    private final ConfirmApplicationUseCase confirmation;
    private final ApplicationQueryUseCase query;

    public CandidateApplicationController(
            ApplicationDraftUseCase drafts, ConfirmApplicationUseCase confirmation, ApplicationQueryUseCase query) {
        this.drafts = drafts;
        this.confirmation = confirmation;
        this.query = query;
    }

    @GetMapping
    List<ApplicationSummary> list(@AuthenticationPrincipal AuthenticatedAccount account) {
        return query.list(account.accountId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> start(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody StartApplicationRequest request,
            HttpServletRequest httpRequest) {
        return Map.of("id", drafts.start(
                account.accountId(), request.processId(), request.positionId(), httpRequest.getRemoteAddr()));
    }

    @GetMapping("/{applicationId}")
    ApplicationDetail get(@AuthenticationPrincipal AuthenticatedAccount account, @PathVariable UUID applicationId) {
        return query.get(account.accountId(), applicationId);
    }

    @DeleteMapping("/{applicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void discard(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable UUID applicationId,
            HttpServletRequest httpRequest) {
        drafts.discard(account.accountId(), applicationId, httpRequest.getRemoteAddr());
    }

    @PostMapping(path = "/{applicationId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> uploadDocument(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable UUID applicationId,
            @RequestParam UUID requirementId,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        return Map.of("id", drafts.uploadDocument(account.accountId(), applicationId, requirementId,
                new MultipartIncomingFile(file), httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/{applicationId}/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeDocument(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable UUID applicationId,
            @PathVariable UUID documentId,
            HttpServletRequest httpRequest) {
        drafts.removeDocument(account.accountId(), applicationId, documentId, httpRequest.getRemoteAddr());
    }

    @GetMapping("/{applicationId}/documents/{documentId}/file")
    ResponseEntity<InputStreamResource> documentFile(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable UUID applicationId,
            @PathVariable UUID documentId) {
        return FileResponses.attachment(query.openDocument(account.accountId(), applicationId, documentId));
    }

    @PostMapping("/{applicationId}/confirm")
    Map<String, String> confirm(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable UUID applicationId,
            HttpServletRequest httpRequest) {
        return Map.of("applicationNumber",
                confirmation.confirm(account.accountId(), applicationId, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/{applicationId}/receipt")
    ResponseEntity<InputStreamResource> receipt(
            @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable UUID applicationId) {
        return FileResponses.attachment(query.receipt(account.accountId(), applicationId));
    }
}
