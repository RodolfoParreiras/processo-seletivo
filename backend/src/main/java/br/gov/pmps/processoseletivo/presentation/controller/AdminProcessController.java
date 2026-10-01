package br.gov.pmps.processoseletivo.presentation.controller;

import br.gov.pmps.processoseletivo.application.dto.PageResponse;
import br.gov.pmps.processoseletivo.application.dto.process.ChangeStageRequest;
import br.gov.pmps.processoseletivo.application.dto.process.DocumentRequirementRequest;
import br.gov.pmps.processoseletivo.application.dto.process.ExtendRegistrationRequest;
import br.gov.pmps.processoseletivo.application.dto.process.PositionRequest;
import br.gov.pmps.processoseletivo.application.dto.process.ProcessDetailResponse;
import br.gov.pmps.processoseletivo.application.dto.process.ProcessDetailsRequest;
import br.gov.pmps.processoseletivo.application.dto.process.ProcessSummaryResponse;
import br.gov.pmps.processoseletivo.application.dto.process.ReasonRequest;
import br.gov.pmps.processoseletivo.application.dto.process.StageHistoryResponse;
import br.gov.pmps.processoseletivo.application.dto.process.StatusHistoryResponse;
import br.gov.pmps.processoseletivo.application.usecase.process.ManageDraftProcessUseCase;
import br.gov.pmps.processoseletivo.application.usecase.process.ProcessLifecycleUseCase;
import br.gov.pmps.processoseletivo.application.usecase.process.ProcessNoticeUseCase;
import br.gov.pmps.processoseletivo.application.usecase.process.ProcessQueryUseCase;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessStatus;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Gestão de processos. Cada operação exige a permissão correspondente (ESPECIFICACAO §6),
 * verificada aqui e não apenas na interface.
 */
@RestController
@RequestMapping("/api/admin/processes")
public class AdminProcessController {

    private final ManageDraftProcessUseCase manageDraft;
    private final ProcessNoticeUseCase notices;
    private final ProcessLifecycleUseCase lifecycle;
    private final ProcessQueryUseCase query;

    public AdminProcessController(
            ManageDraftProcessUseCase manageDraft,
            ProcessNoticeUseCase notices,
            ProcessLifecycleUseCase lifecycle,
            ProcessQueryUseCase query) {
        this.manageDraft = manageDraft;
        this.notices = notices;
        this.lifecycle = lifecycle;
        this.query = query;
    }

    // ---- Consulta ----

    @GetMapping
    @PreAuthorize("hasAuthority('PROCESSO_VISUALIZAR')")
    PageResponse<ProcessSummaryResponse> list(
            @RequestParam(required = false) ProcessStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return query.listAdmin(status, page, size);
    }

    @GetMapping("/{processId}")
    @PreAuthorize("hasAuthority('PROCESSO_VISUALIZAR')")
    ProcessDetailResponse get(@PathVariable UUID processId) {
        return query.getAdmin(processId);
    }

    @GetMapping("/{processId}/history")
    @PreAuthorize("hasAuthority('PROCESSO_VISUALIZAR')")
    List<StatusHistoryResponse> history(@PathVariable UUID processId) {
        return query.history(processId);
    }

    @GetMapping("/{processId}/stage-history")
    @PreAuthorize("hasAuthority('PROCESSO_VISUALIZAR')")
    List<StageHistoryResponse> stageHistory(@PathVariable UUID processId) {
        return query.stageHistory(processId);
    }

    @GetMapping("/{processId}/notices/{version}/file")
    @PreAuthorize("hasAuthority('PROCESSO_VISUALIZAR')")
    ResponseEntity<InputStreamResource> noticeFile(@PathVariable UUID processId, @PathVariable int version) {
        return FileResponses.attachment(query.openAdminNotice(processId, version));
    }

    // ---- Rascunho ----

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROCESSO_CRIAR')")
    Map<String, UUID> create(
            @Valid @RequestBody ProcessDetailsRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        return Map.of("id", manageDraft.create(request, account.accountId(), httpRequest.getRemoteAddr()));
    }

    @PutMapping("/{processId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROCESSO_EDITAR')")
    void update(
            @PathVariable UUID processId,
            @Valid @RequestBody ProcessDetailsRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        manageDraft.updateDetails(processId, request, account.accountId(), httpRequest.getRemoteAddr());
    }

    @PostMapping("/{processId}/positions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROCESSO_EDITAR')")
    Map<String, UUID> addPosition(
            @PathVariable UUID processId,
            @Valid @RequestBody PositionRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        return Map.of("id", manageDraft.addPosition(processId, request, account.accountId(), httpRequest.getRemoteAddr()));
    }

    @PutMapping("/{processId}/positions/{positionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROCESSO_EDITAR')")
    void updatePosition(
            @PathVariable UUID processId,
            @PathVariable UUID positionId,
            @Valid @RequestBody PositionRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        manageDraft.updatePosition(processId, positionId, request, account.accountId(), httpRequest.getRemoteAddr());
    }

    @DeleteMapping("/{processId}/positions/{positionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROCESSO_EDITAR')")
    void removePosition(
            @PathVariable UUID processId,
            @PathVariable UUID positionId,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        manageDraft.removePosition(processId, positionId, account.accountId(), httpRequest.getRemoteAddr());
    }

    @PostMapping("/{processId}/document-requirements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROCESSO_EDITAR')")
    Map<String, UUID> addDocumentRequirement(
            @PathVariable UUID processId,
            @Valid @RequestBody DocumentRequirementRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        return Map.of("id", manageDraft.addDocumentRequirement(
                processId, request, account.accountId(), httpRequest.getRemoteAddr()));
    }

    @PutMapping("/{processId}/document-requirements/{requirementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROCESSO_EDITAR')")
    void updateDocumentRequirement(
            @PathVariable UUID processId,
            @PathVariable UUID requirementId,
            @Valid @RequestBody DocumentRequirementRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        manageDraft.updateDocumentRequirement(
                processId, requirementId, request, account.accountId(), httpRequest.getRemoteAddr());
    }

    @DeleteMapping("/{processId}/document-requirements/{requirementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROCESSO_EDITAR')")
    void removeDocumentRequirement(
            @PathVariable UUID processId,
            @PathVariable UUID requirementId,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        manageDraft.removeDocumentRequirement(processId, requirementId, account.accountId(), httpRequest.getRemoteAddr());
    }

    /** Em rascunho, troca o arquivo; após a publicação, cria retificação e exige {@code reason}. */
    @PostMapping(path = "/{processId}/notices", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROCESSO_EDITAR')")
    void uploadNotice(
            @PathVariable UUID processId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        notices.upload(processId, new MultipartIncomingFile(file), reason, account.accountId(), httpRequest.getRemoteAddr());
    }

    // ---- Ciclo de vida ----

    @PostMapping("/{processId}/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROCESSO_PUBLICAR')")
    void publish(
            @PathVariable UUID processId,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        lifecycle.publish(processId, account.accountId(), httpRequest.getRemoteAddr());
    }

    /** Etapa de divulgação definida manualmente (docs/DECISOES.md). */
    @PostMapping("/{processId}/stage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('RESULTADO_PUBLICAR')")
    void changeStage(
            @PathVariable UUID processId,
            @Valid @RequestBody ChangeStageRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        lifecycle.changeStage(processId, request.stage(), account.accountId(), httpRequest.getRemoteAddr());
    }

    @PostMapping("/{processId}/extend-registration")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROCESSO_EDITAR')")
    void extendRegistration(
            @PathVariable UUID processId,
            @Valid @RequestBody ExtendRegistrationRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        lifecycle.extendRegistration(processId, request.newRegistrationEnd(), request.reason(),
                account.accountId(), httpRequest.getRemoteAddr());
    }

    @PostMapping("/{processId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROCESSO_ENCERRAR')")
    void suspend(
            @PathVariable UUID processId,
            @Valid @RequestBody ReasonRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        lifecycle.suspend(processId, request.reason(), account.accountId(), httpRequest.getRemoteAddr());
    }

    @PostMapping("/{processId}/resume")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROCESSO_ENCERRAR')")
    void resume(
            @PathVariable UUID processId,
            @Valid @RequestBody ReasonRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        lifecycle.resume(processId, request.reason(), account.accountId(), httpRequest.getRemoteAddr());
    }

    @PostMapping("/{processId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROCESSO_ENCERRAR')")
    void cancel(
            @PathVariable UUID processId,
            @Valid @RequestBody ReasonRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        lifecycle.cancel(processId, request.reason(), account.accountId(), httpRequest.getRemoteAddr());
    }

    @PostMapping("/{processId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROCESSO_ENCERRAR')")
    void archive(
            @PathVariable UUID processId,
            @Valid @RequestBody ReasonRequest request,
            @AuthenticationPrincipal AuthenticatedAccount account,
            HttpServletRequest httpRequest) {
        lifecycle.archive(processId, request.reason(), account.accountId(), httpRequest.getRemoteAddr());
    }
}
