package br.gov.pmps.processoseletivo.presentation.controller;

import br.gov.pmps.processoseletivo.application.dto.AuthenticationResult;
import br.gov.pmps.processoseletivo.application.dto.ForgotPasswordRequest;
import br.gov.pmps.processoseletivo.application.dto.LoginRequest;
import br.gov.pmps.processoseletivo.application.dto.SessionResponse;
import br.gov.pmps.processoseletivo.application.usecase.LoginUseCase;
import br.gov.pmps.processoseletivo.application.usecase.RequestPasswordResetUseCase;
import br.gov.pmps.processoseletivo.domain.model.AccountType;
import br.gov.pmps.processoseletivo.security.SessionLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Login da área administrativa: contas separadas das contas de candidato. */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final LoginUseCase login;
    private final RequestPasswordResetUseCase requestPasswordReset;
    private final SessionLoginService sessionLoginService;

    public AdminAuthController(
            LoginUseCase login, RequestPasswordResetUseCase requestPasswordReset, SessionLoginService sessionLoginService) {
        this.login = login;
        this.requestPasswordReset = requestPasswordReset;
        this.sessionLoginService = sessionLoginService;
    }

    @PostMapping("/login")
    SessionResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthenticationResult result = login.execute(AccountType.ADMIN, request, httpRequest.getRemoteAddr());
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
