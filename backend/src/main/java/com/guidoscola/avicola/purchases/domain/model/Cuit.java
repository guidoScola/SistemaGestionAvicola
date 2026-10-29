package com.guidoscola.avicola.purchases.domain.model;

import com.guidoscola.avicola.shared.domain.InvalidInputException;

import java.util.regex.Pattern;

/**
 * Argentine tax id (Clave Única de Identificación Tributaria): 11 digits whose last one is a modulo-11 check digit.
 * Stored without separators.
 */
public record Cuit(String value) {

    private static final Pattern DIGITS = Pattern.compile("\\d{11}");
    private static final int[] WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    public Cuit {
        String digits = value == null ? "" : value.replaceAll("[\\s-]", "");
        if (!DIGITS.matcher(digits).matches() || checkDigit(digits) != digits.charAt(10) - '0') {
            throw new InvalidInputException("Invalid CUIT");
        }
        value = digits;
    }

    public String formatted() {
        return value.substring(0, 2) + "-" + value.substring(2, 10) + "-" + value.substring(10);
    }

    private static int checkDigit(String digits) {
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += (digits.charAt(i) - '0') * WEIGHTS[i];
        }
        int result = 11 - sum % 11;
        // 11 maps to 0; 10 has no valid check digit.
        return result == 11 ? 0 : result == 10 ? -1 : result;
    }
}
