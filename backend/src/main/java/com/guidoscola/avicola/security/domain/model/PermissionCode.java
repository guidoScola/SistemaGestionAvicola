package com.guidoscola.avicola.security.domain.model;

import com.guidoscola.avicola.shared.domain.InvalidInputException;

import java.util.regex.Pattern;

public record PermissionCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)*");

    public PermissionCode {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new InvalidInputException("Invalid permission code: " + value);
        }
    }
}
