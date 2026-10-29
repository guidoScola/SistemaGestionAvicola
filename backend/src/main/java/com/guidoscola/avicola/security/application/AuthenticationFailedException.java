package com.guidoscola.avicola.security.application;

import com.guidoscola.avicola.shared.domain.DomainException;

public class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException() {
        super("Invalid credentials");
    }
}
