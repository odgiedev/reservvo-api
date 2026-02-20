package com.odgiedev.reservvo.exception.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotBlankOrSymbolsValidator implements ConstraintValidator<NotBlankOrSymbols, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return false;
        // verifica se contém pelo menos uma letra ou número
        return value.chars().anyMatch(Character::isLetterOrDigit);
    }
}