package com.odgiedev.reservvo.exception.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidPhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhone {
    String message() default "Telefone inválido. Informe 6 a 20 dígitos (ex: 11987654321).";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
