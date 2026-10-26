package com.guidoscola.avicola.security.application;

import com.guidoscola.avicola.security.application.port.PasswordHasher;

class FakePasswordHasher implements PasswordHasher {

    @Override
    public String hash(String rawPassword) {
        return "hashed:" + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return hash.equals(hash(rawPassword));
    }
}
