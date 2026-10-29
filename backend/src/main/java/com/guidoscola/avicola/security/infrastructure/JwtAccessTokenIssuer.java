package com.guidoscola.avicola.security.infrastructure;

import com.guidoscola.avicola.security.application.AccessToken;
import com.guidoscola.avicola.security.application.port.AccessTokenIssuer;
import com.guidoscola.avicola.security.domain.model.PermissionCode;
import com.guidoscola.avicola.security.domain.model.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

class JwtAccessTokenIssuer implements AccessTokenIssuer {

    static final String ISSUER = "avicola";
    static final String PERMISSIONS_CLAIM = "permissions";

    private final JwtEncoder encoder;
    private final Clock clock;
    private final Duration ttl;

    JwtAccessTokenIssuer(JwtEncoder encoder, Clock clock, Duration ttl) {
        this.encoder = encoder;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Override
    public AccessToken issue(User user, Set<PermissionCode> permissions) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.id().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("username", user.username().value())
                .claim("fullName", user.fullName())
                .claim(PERMISSIONS_CLAIM, permissions.stream().map(PermissionCode::value).sorted().toList())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, expiresAt);
    }
}
