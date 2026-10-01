package br.gov.pmps.processoseletivo.presentation.controller;

import br.gov.pmps.processoseletivo.application.usecase.export.ApplicationExportUseCase;
import br.gov.pmps.processoseletivo.application.usecase.export.ApplicationExportUseCase.Kind;
import br.gov.pmps.processoseletivo.application.usecase.export.ApplicationExportUseCase.PreparedExport;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationStatus;
import br.gov.pmps.processoseletivo.security.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Exportação para a classificação externa e relatórios (ESPECIFICACAO §31, §34 e §35). */
@RestController
@RequestMapping("/api/admin/processes/{processId}")
public class AdminExportController {

    private final ApplicationExportUseCase exports;

    public AdminExportController(ApplicationExportUseCase exports) {
        this.exports = exports;
    }

    @GetMapping("/exports/applications.xlsx")
    @PreAuthorize("hasAuthority('EXPORTACAO_GERAR')")
    ResponseEntity<StreamingResponseBody> classificationSpreadsheet(
            @PathVariable UUID processId,
            @RequestParam(required = false) ApplicationStatus status,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        boolean includeDisabilityData = authentication.getAuthorities().stream()
                .anyMatch(authority -> AdminApplicationController.DISABILITY_DATA_PERMISSION.equals(authority.getAuthority()));
        return stream(exports.prepare(Kind.CLASSIFICATION_SPREADSHEET, processId, status, includeDisabilityData,
                accountId(authentication), httpRequest.getRemoteAddr()));
    }

    @GetMapping("/reports/applications.pdf")
    @PreAuthorize("hasAuthority('RELATORIO_GERAR')")
    ResponseEntity<StreamingResponseBody> applicationsReport(
            @PathVariable UUID processId,
            @RequestParam(required = false) ApplicationStatus status,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        // O relatório não inclui dados de pessoa com deficiência, independentemente da permissão.
        return stream(exports.prepare(Kind.APPLICATIONS_REPORT, processId, status, false,
                accountId(authentication), httpRequest.getRemoteAddr()));
    }

    private static ResponseEntity<StreamingResponseBody> stream(PreparedExport export) {
        StreamingResponseBody body = output -> export.writer().writeTo(output);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(export.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(export.fileName()).build().toString())
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .body(body);
    }

    private static UUID accountId(Authentication authentication) {
        return ((AuthenticatedAccount) authentication.getPrincipal()).accountId();
    }
}
