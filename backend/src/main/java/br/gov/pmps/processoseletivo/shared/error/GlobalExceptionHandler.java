package br.gov.pmps.processoseletivo.shared.error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Padroniza erros da API em ProblemDetail (RFC 9457) sem expor detalhes internos (ESPECIFICACAO §64).
 * Exceções do Spring MVC são tratadas pela classe base; as demais viram uma resposta 500 genérica.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(BusinessException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        if (!exception.getViolations().isEmpty()) {
            problem.setProperty("violations", exception.getViolations());
        }
        return problem;
    }

    /** Devolve apenas o campo e a mensagem de validação, nunca o valor recebido. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Verifique os campos informados.");
        problem.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(DomainRuleException.class)
    ProblemDetail handleDomainRule(DomainRuleException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    /** Duas alterações simultâneas no mesmo registro: a segunda é recusada em vez de sobrescrever a primeira. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "O registro foi alterado por outra pessoa. Recarregue a página e tente novamente.");
    }

    /** Violação de constraint (ex.: duas requisições simultâneas com o mesmo dado único). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Violação de integridade: {}", exception.getMostSpecificCause().getClass().getSimpleName());
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Não foi possível concluir a operação: os dados conflitam com um registro existente.");
    }

    /** Devolve ao Spring Security, que responde 401/403; tratá-las aqui viraria um 500. */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    void rethrowSecurityException(RuntimeException exception) {
        throw exception;
    }

    /** Corpo ilegível (JSON inválido, identificador malformado): mensagem em português, sem detalhes do parser. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Requisição inválida. Verifique os dados enviados."));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Parâmetro inválido na requisição."));
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        // O identificador permite relacionar o relato do usuário ao log sem devolver detalhes técnicos.
        String errorId = UUID.randomUUID().toString();
        log.error("Erro não tratado [errorId={}]", errorId, exception);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Não foi possível processar a solicitação. Tente novamente mais tarde.");
        problem.setTitle("Erro interno");
        problem.setProperty("errorId", errorId);
        return problem;
    }
}
