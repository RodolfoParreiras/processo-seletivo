package br.gov.pmps.processoseletivo.application.dto;

import br.gov.pmps.processoseletivo.domain.rule.Cpf;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCpf.Validator.class)
public @interface ValidCpf {

    String message() default "CPF inválido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidCpf, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return Cpf.isValid(value);
        }
    }
}
