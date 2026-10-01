package br.gov.pmps.processoseletivo.presentation.controller;

import br.gov.pmps.processoseletivo.application.dto.AuthenticationResult;
import br.gov.pmps.processoseletivo.application.dto.ForgotPasswordRequest;
import br.gov.pmps.processoseletivo.application.dto.LoginRequest;
import br.gov.pmps.processoseletivo.application.dto.SessionResponse;
import br.gov.pmps.processoseletivo.application.usecase.LoginUseCase;
import br.gov.pmps.processoseletivo.application.usecase.RequestPasswordResetUseCase;
import br.gov.pmps.processoseletivo.domain.model.AccountType;
import br.gov.pmps.processoseletivo.security.SessionLoginService;
import br.gov.pmps.processoseletivo.security.mfa.AdminMfaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login da área administrativa em duas etapas: senha e segundo fator (TOTP) obrigatório
 * (ESPECIFICACAO §76). Contas separadas das contas de candidato.
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    public record MfaChallengeResponse(boolean mfaRequired, boolean enrollmentRequired) {
    }

    public record MfaCodeRequest(@NotBlank(message = "Informe o código.") @Size(max = 10) String code) {
    }

    private final LoginUseCase login;
    private final RequestPasswordResetUseCase requestPasswordReset;
    private final AdminMfaService mfaService;
    private final SessionLoginService sessionLoginService;

    public AdminAuthController(
            LoginUseCase login,
            RequestPasswordResetUseCase requestPasswordReset,
            AdminMfaService mfaService,
            SessionLoginService sessionLoginService) {
        this.login = login;
        this.requestPasswordReset = requestPasswordReset;
        this.mfaService = mfaService;
        this.sessionLoginService = sessionLoginService;
    }

    /** Primeira etapa: senha. Não concede acesso; apenas abre o desafio do segundo fator. */
    @PostMapping("/login")
    MfaChallengeResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthenticationResult result = login.execute(AccountType.ADMIN, request, httpRequest.getRemoteAddr());
        return new MfaChallengeResponse(true, mfaService.startChallenge(result, httpRequest));
    }

    /** Para quem ainda não tem o aplicativo autenticador cadastrado. */
    @PostMapping("/mfa/setup")
    AdminMfaService.SetupResponse setup(HttpServletRequest httpRequest) {
        return mfaService.setup(httpRequest);
    }

    /** Segunda etapa: código do aplicativo. Só aqui a sessão autenticada é criada. */
    @PostMapping("/mfa/verify")
    SessionResponse verify(
            @Valid @RequestBody MfaCodeRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthenticationResult result = mfaService.verify(request.code().trim(), httpRequest);
        sessionLoginService.establish(result, httpRequest, httpResponse);
        return new SessionResponse(result.accountType(), result.displayName(), result.permissions());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        requestPasswordReset.execute(AccountType.ADMIN, request.email(), httpRequest.getRemoteAddr());
        return Map.of("message", AuthController.FORGOT_PASSWORD_MESSAGE);
    }
}
