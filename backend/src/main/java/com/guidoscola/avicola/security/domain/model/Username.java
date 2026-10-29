package com.guidoscola.avicola.security.domain.model;

import com.guidoscola.avicola.shared.domain.InvalidInputException;

import java.util.Locale;
import java.util.regex.Pattern;

public record Username(String value) {

    private static final Pattern FORMAT = Pattern.compile("[a-z0-9._-]{3,50}");

    public Username {
        if (value == null) {
            throw new InvalidInputException("Username is required");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidInputException(
                    "Username must be 3-50 characters: letters, digits, dot, underscore or hyphen");
        }
    }
}
