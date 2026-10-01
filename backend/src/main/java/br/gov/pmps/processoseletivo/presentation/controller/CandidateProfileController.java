package br.gov.pmps.processoseletivo.presentation.controller;

import br.gov.pmps.processoseletivo.application.dto.CandidateProfileRequest;
import br.gov.pmps.processoseletivo.application.dto.CandidateProfileResponse;
import br.gov.pmps.processoseletivo.application.dto.ChangeEmailRequest;
import br.gov.pmps.processoseletivo.application.usecase.CandidateProfileUseCase;
import br.gov.pmps.processoseletivo.application.usecase.ChangeEmailUseCase;
import br.gov.pmps.processoseletivo.security.AuthenticatedAccount;
import br.gov.pmps.processoseletivo.security.SessionLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Dados do candidato autenticado. Não há identificador na URL: o recurso é sempre o do titular da sessão. */
@RestController
@RequestMapping("/api/candidate/me")
public class CandidateProfileController {

    private final CandidateProfileUseCase candidateProfile;
    private final ChangeEmailUseCase changeEmail;
    private final SessionLoginService sessionLoginService;

    public CandidateProfileController(
            CandidateProfileUseCase candidateProfile,
            ChangeEmailUseCase changeEmail,
            SessionLoginService sessionLoginService) {
        this.candidateProfile = candidateProfile;
        this.changeEmail = changeEmail;
        this.sessionLoginService = sessionLoginService;
    }

    @GetMapping
    CandidateProfileResponse get(@AuthenticationPrincipal AuthenticatedAccount account) {
        return candidateProfile.get(account.accountId());
    }

    @PutMapping
    CandidateProfileResponse update(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody CandidateProfileRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        CandidateProfileResponse updated =
                candidateProfile.update(account.accountId(), request, httpRequest.getRemoteAddr());
        sessionLoginService.updateDisplayName(updated.fullName(), httpRequest, httpResponse);
        return updated;
    }

    @PutMapping("/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changeEmail(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody ChangeEmailRequest request,
            HttpServletRequest httpRequest) {
        changeEmail.execute(account.accountId(), request, httpRequest.getRemoteAddr());
    }
}
