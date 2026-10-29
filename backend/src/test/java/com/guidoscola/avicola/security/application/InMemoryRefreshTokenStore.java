package com.guidoscola.avicola.security.application;

import com.guidoscola.avicola.security.application.port.RefreshTokenStore;
import com.guidoscola.avicola.security.domain.model.RefreshToken;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<UUID, RefreshToken> tokens = new LinkedHashMap<>();

    @Override
    public RefreshToken save(RefreshToken token) {
        tokens.put(token.id(), token);
        return token;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return tokens.values().stream().filter(t -> t.tokenHash().equals(tokenHash)).findFirst();
    }

    @Override
    public boolean revoke(UUID tokenId) {
        RefreshToken token = tokens.get(tokenId);
        if (token == null || token.revoked()) {
            return false;
        }
        tokens.put(tokenId, token.revoke());
        return true;
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        tokens.replaceAll((id, t) -> t.userId().equals(userId) ? t.revoke() : t);
    }

    List<RefreshToken> all() {
        return List.copyOf(tokens.values());
    }
}
