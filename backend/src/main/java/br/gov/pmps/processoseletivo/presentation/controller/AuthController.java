package br.gov.pmps.processoseletivo.presentation.controller;

import br.gov.pmps.processoseletivo.application.dto.AuthenticationResult;
import br.gov.pmps.processoseletivo.application.dto.ChangePasswordRequest;
import br.gov.pmps.processoseletivo.application.dto.ForgotPasswordRequest;
import br.gov.pmps.processoseletivo.application.dto.LoginRequest;
import br.gov.pmps.processoseletivo.application.dto.RegisterCandidateRequest;
import br.gov.pmps.processoseletivo.application.dto.ResetPasswordRequest;
import br.gov.pmps.processoseletivo.application.dto.SessionResponse;
import br.gov.pmps.processoseletivo.application.usecase.ChangePasswordUseCase;
import br.gov.pmps.processoseletivo.application.usecase.LoginUseCase;
import br.gov.pmps.processoseletivo.application.usecase.RegisterCandidateUseCase;
import br.gov.pmps.processoseletivo.application.usecase.RequestPasswordResetUseCase;
import br.gov.pmps.processoseletivo.application.usecase.ResetPasswordUseCase;
import br.gov.pmps.processoseletivo.domain.model.AccountType;
import br.gov.pmps.processoseletivo.security.AuthenticatedAccount;
import br.gov.pmps.processoseletivo.security.SessionLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Autenticação da área do candidato e operações comuns às duas áreas (sessão, logout, nova senha). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String FORGOT_PASSWORD_MESSAGE =
            "Se o e-mail estiver cadastrado, você receberá um link para redefinir a senha.";

    private final RegisterCandidateUseCase registerCandidate;
    private final LoginUseCase login;
    private final RequestPasswordResetUseCase requestPasswordReset;
    private final ResetPasswordUseCase resetPassword;
    private final ChangePasswordUseCase changePassword;
    private final SessionLoginService sessionLoginService;

    public AuthController(
            RegisterCandidateUseCase registerCandidate,
            LoginUseCase login,
            RequestPasswordResetUseCase requestPasswordReset,
            ResetPasswordUseCase resetPassword,
            ChangePasswordUseCase changePassword,
            SessionLoginService sessionLoginService) {
        this.registerCandidate = registerCandidate;
        this.login = login;
        this.requestPasswordReset = requestPasswordReset;
        this.resetPassword = resetPassword;
        this.changePassword = changePassword;
        this.sessionLoginService = sessionLoginService;
    }

    /** Força a emissão do cookie XSRF-TOKEN, que o frontend reenvia no header X-XSRF-TOKEN. */
    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    void register(@Valid @RequestBody RegisterCandidateRequest request, HttpServletRequest httpRequest) {
        registerCandidate.execute(request, httpRequest.getRemoteAddr());
    }

    @PostMapping("/login")
    SessionResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthenticationResult result = login.execute(AccountType.CANDIDATE, request, httpRequest.getRemoteAddr());
        sessionLoginService.establish(result, httpRequest, httpResponse);
        return new SessionResponse(result.accountType(), result.displayName(), result.permissions());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(Authentication authentication, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        sessionLoginService.terminate((AuthenticatedAccount) authentication.getPrincipal(), httpRequest, httpResponse);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        requestPasswordReset.execute(AccountType.CANDIDATE, request.email(), httpRequest.getRemoteAddr());
        return Map.of("message", FORGOT_PASSWORD_MESSAGE);
    }

    /** Atende candidatos e administradores: o token identifica a conta. */
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
        resetPassword.execute(request, httpRequest.getRemoteAddr());
    }

    /** Troca de senha com sessão ativa; as demais sessões da conta são encerradas. */
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        AuthenticatedAccount account = (AuthenticatedAccount) authentication.getPrincipal();
        changePassword.execute(
                account.accountId(), request, httpRequest.getSession().getId(), httpRequest.getRemoteAddr());
    }

    @GetMapping("/session")
    SessionResponse session(Authentication authentication) {
        AuthenticatedAccount account = (AuthenticatedAccount) authentication.getPrincipal();
        Set<String> permissions = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> !authority.startsWith("ROLE_"))
                .collect(Collectors.toUnmodifiableSet());
        return new SessionResponse(account.accountType(), account.displayName(), permissions);
    }
}
