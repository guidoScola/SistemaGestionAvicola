package com.guidoscola.avicola.security.application;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {
}
