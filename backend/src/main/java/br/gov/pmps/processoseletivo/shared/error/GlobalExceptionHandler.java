package br.gov.pmps.processoseletivo.shared.error;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Padroniza erros da API em ProblemDetail (RFC 9457) sem expor detalhes internos (ESPECIFICACAO §64).
 * Exceções do Spring MVC são tratadas pela classe base; as demais viram uma resposta 500 genérica.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
