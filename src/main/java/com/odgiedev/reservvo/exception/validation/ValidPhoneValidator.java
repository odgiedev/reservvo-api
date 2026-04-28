package com.odgiedev.reservvo.exception.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private static final int MIN_DIGITS = 6;
    private static final int MAX_DIGITS = 20;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;

        if (!value.matches("^[+0-9\\s()\\-]+$")) return false;

        String digits = value.replaceAll("\\D", "");
        int len = digits.length();
        return len >= MIN_DIGITS && len <= MAX_DIGITS;
    }
}
