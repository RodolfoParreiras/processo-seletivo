package br.gov.pmps.processoseletivo.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unexpectedErrorReturnsGenericMessageWithoutInternalDetails() {
        ProblemDetail problem = handler.handleUnexpected(
                new IllegalStateException("SELECT * FROM app.candidate WHERE cpf = '12345678900'"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail())
                .doesNotContain("SELECT")
                .doesNotContain("IllegalStateException")
                .doesNotContain("12345678900");
        assertThat(problem.getProperties()).containsKey("errorId");
    }
}
