package com.guidoscola.avicola.security.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ConfigurationProperties("avicola.security")
record AuthProperties(
        String jwtSecret,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("7d") Duration refreshTokenTtl,
        @DefaultValue("true") boolean cookieSecure) {

    static final int MIN_SECRET_BYTES = 32;

    AuthProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "avicola.security.jwt-secret (JWT_SECRET) must be set to at least " + MIN_SECRET_BYTES + " bytes");
        }
    }
}
