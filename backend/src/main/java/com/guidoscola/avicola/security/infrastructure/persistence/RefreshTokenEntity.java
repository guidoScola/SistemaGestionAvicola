package com.guidoscola.avicola.security.infrastructure.persistence;

import com.guidoscola.avicola.security.domain.model.RefreshToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    protected RefreshTokenEntity() {
    }

    static RefreshTokenEntity fromDomain(RefreshToken token) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.id = token.id();
        entity.userId = token.userId();
        entity.tokenHash = token.tokenHash();
        entity.expiresAt = token.expiresAt();
        entity.revoked = token.revoked();
        return entity;
    }

    RefreshToken toDomain() {
        return new RefreshToken(id, userId, tokenHash, expiresAt, revoked);
    }
}
