package com.odgiedev.reservvo.util;

public class StringUtils {
    private StringUtils() {}

    public static String trimToNull(String value) {
        if (value == null) return null;

        value = value.trim();

        return value.isEmpty() ? null : value;
    }

    public static String normalizePhone(String value) {
        if (value == null) return null;

        String digits = value.replaceAll("\\D", "");

        return digits.isEmpty() ? null : digits;
    }

    public static String capitalizeWords(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;

        String[] parts = trimmed.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(" ");
            String p = parts[i];
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    public static String normalizeEmail(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase();
    }
}
