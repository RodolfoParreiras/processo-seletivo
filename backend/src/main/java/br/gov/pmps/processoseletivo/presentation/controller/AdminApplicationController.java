package br.gov.pmps.processoseletivo.presentation.controller;

import br.gov.pmps.processoseletivo.application.dto.PageResponse;
import br.gov.pmps.processoseletivo.application.dto.application.AdminApplicationDtos.AdminApplicationDetail;
import br.gov.pmps.processoseletivo.application.dto.application.AdminApplicationDtos.DecisionRequest;
import br.gov.pmps.processoseletivo.application.usecase.application.AdminApplicationUseCase;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationStatus;
import br.gov.pmps.processoseletivo.domain.repository.AdminApplicationRow;
import br.gov.pmps.processoseletivo.presentation.FileResponses;
import br.gov.pmps.processoseletivo.security.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Inscrições na área administrativa: consulta, documentos e deferimento/indeferimento. */
@RestController
@RequestMapping("/api/admin")
public class AdminApplicationController {

    static final String DISABILITY_DATA_PERMISSION = "DADOS_PCD_VISUALIZAR";

    private final AdminApplicationUseCase applications;

    public AdminApplicationController(AdminApplicationUseCase applications) {
        this.applications = applications;
    }

    @GetMapping("/processes/{processId}/applications")
    @PreAuthorize("hasAuthority('INSCRICAO_VISUALIZAR')")
    PageResponse<AdminApplicationRow> list(
            @PathVariable UUID processId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return applications.list(processId, status, page, size);
    }

    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("hasAuthority('INSCRICAO_VISUALIZAR')")
    AdminApplicationDetail get(
            @PathVariable UUID applicationId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        boolean canViewDisabilityData = authentication.getAuthorities().stream()
                .anyMatch(authority -> DISABILITY_DATA_PERMISSION.equals(authority.getAuthority()));
        AuthenticatedAccount account = (AuthenticatedAccount) authentication.getPrincipal();
        return applications.get(applicationId, canViewDisabilityData, account.accountId(), httpRequest.getRemoteAddr());
    }

    @GetMapping("/applications/{applicationId}/documents/{documentId}/file")
    @PreAuthorize("hasAuthority('INSCRICAO_VISUALIZAR')")
    ResponseEntity<InputStreamResource> documentFile(
            @PathVariable UUID applicationId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        return FileResponses.attachment(
                applications.openDocument(applicationId, documentId, account.accountId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/applications/{applicationId}/defer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('INSCRICAO_DEFERIR')")
    void defer(
            @PathVariable UUID applicationId,
            @Valid @RequestBody DecisionRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        applications.decide(applicationId, ApplicationStatus.DEFERIDA, request.reason(),
                account.accountId(), httpRequest.getRemoteAddr());
    }

    @PostMapping("/applications/{applicationId}/deny")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('INSCRICAO_INDEFERIR')")
    void deny(
            @PathVariable UUID applicationId,
            @Valid @RequestBody DecisionRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        applications.decide(applicationId, ApplicationStatus.INDEFERIDA, request.reason(),
                account.accountId(), httpRequest.getRemoteAddr());
    }
}
