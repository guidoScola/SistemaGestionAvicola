package com.guidoscola.avicola.security.application.port;

import com.guidoscola.avicola.security.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStore {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Atomically revokes an active token.
     *
     * @return true only for the caller that moved the token from active to revoked
     */
    boolean revoke(UUID tokenId);

    void revokeAllForUser(UUID userId);
}
