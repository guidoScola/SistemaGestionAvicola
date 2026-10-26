package com.guidoscola.avicola.security.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A refresh token as persisted: only the hash of the raw token is ever stored.
 */
public record RefreshToken(UUID id, UUID userId, String tokenHash, Instant expiresAt, boolean revoked) {

    public static RefreshToken issue(UUID userId, String tokenHash, Instant expiresAt) {
        return new RefreshToken(UUID.randomUUID(), userId, tokenHash, expiresAt, false);
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public RefreshToken revoke() {
        return new RefreshToken(id, userId, tokenHash, expiresAt, true);
    }
}
