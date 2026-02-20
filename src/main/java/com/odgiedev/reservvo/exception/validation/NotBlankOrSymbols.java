package com.odgiedev.reservvo.exception.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotBlankOrSymbolsValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotBlankOrSymbols {
    String message() default "Campo deve conter letras ou números";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}