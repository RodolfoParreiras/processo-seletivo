package br.gov.pmps.processoseletivo.shared.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Erro esperado de regra de negócio. A mensagem é exibida ao usuário,
 * portanto nunca deve conter dados internos.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> violations;

    public BusinessException(HttpStatus status, String message) {
        this(status, message, List.of());
    }

    public BusinessException(HttpStatus status, String message, List<String> violations) {
        super(message);
        this.status = status;
        this.violations = List.copyOf(violations);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<String> getViolations() {
        return violations;
    }
}
