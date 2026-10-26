package com.guidoscola.avicola.security.infrastructure.persistence;

import com.guidoscola.avicola.security.application.port.RefreshTokenStore;
import com.guidoscola.avicola.security.domain.model.RefreshToken;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class JpaRefreshTokenStore implements RefreshTokenStore {

    private final RefreshTokenJpaRepository jpa;

    JpaRefreshTokenStore(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return jpa.save(RefreshTokenEntity.fromDomain(token)).toDomain();
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(RefreshTokenEntity::toDomain);
    }

    @Override
    public boolean revoke(UUID tokenId) {
        return jpa.revokeIfActive(tokenId) == 1;
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        jpa.revokeAllForUser(userId);
    }
}
