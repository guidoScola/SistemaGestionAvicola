package com.guidoscola.avicola.security.application;

import com.guidoscola.avicola.security.application.port.AccessTokenIssuer;
import com.guidoscola.avicola.security.application.port.PasswordHasher;
import com.guidoscola.avicola.security.application.port.RefreshTokenStore;
import com.guidoscola.avicola.security.application.port.RoleRepository;
import com.guidoscola.avicola.security.application.port.UserRepository;
import com.guidoscola.avicola.security.domain.model.PermissionCode;
import com.guidoscola.avicola.security.domain.model.RefreshToken;
import com.guidoscola.avicola.security.domain.model.Role;
import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.security.domain.model.Username;
import com.guidoscola.avicola.shared.domain.InvalidInputException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Login, refresh-token rotation and logout.
 * Every failure surfaces as {@link AuthenticationFailedException} so callers cannot tell which check failed.
 */
public class AuthenticationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenStore refreshTokens;
    private final AccessTokenIssuer accessTokens;
    private final Clock clock;
    private final Duration refreshTtl;

    public AuthenticationService(UserRepository users, RoleRepository roles, PasswordHasher passwordHasher,
                                 RefreshTokenStore refreshTokens, AccessTokenIssuer accessTokens,
                                 Clock clock, Duration refreshTtl) {
        this.users = users;
        this.roles = roles;
        this.passwordHasher = passwordHasher;
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.clock = clock;
        this.refreshTtl = refreshTtl;
    }

    public AuthSession login(String username, String password) {
        User user = findByUsername(username)
                .filter(User::isActive)
                .filter(u -> password != null && passwordHasher.matches(password, u.passwordHash()))
                .orElseThrow(AuthenticationFailedException::new);
        return openSession(user);
    }

    public AuthSession refresh(String rawRefreshToken) {
        RefreshToken token = findToken(rawRefreshToken).orElseThrow(AuthenticationFailedException::new);
        if (token.isExpired(clock.instant()) && !token.revoked()) {
            throw new AuthenticationFailedException();
        }
        if (token.revoked() || !refreshTokens.revoke(token.id())) {
            // A rotated token was presented again (or raced with another refresh):
            // treat it as stolen and end every session of the user.
            refreshTokens.revokeAllForUser(token.userId());
            throw new AuthenticationFailedException();
        }
        User user = users.findById(token.userId())
                .filter(User::isActive)
                .orElseThrow(AuthenticationFailedException::new);
        return openSession(user);
    }

    public void logout(String rawRefreshToken) {
        findToken(rawRefreshToken).ifPresent(token -> refreshTokens.revoke(token.id()));
    }

    private AuthSession openSession(User user) {
        Set<PermissionCode> permissions = roles.findAllById(user.roleIds()).stream()
                .map(Role::permissions)
                .flatMap(Set::stream)
                .collect(Collectors.toUnmodifiableSet());
        String rawRefreshToken = newRawToken();
        Instant refreshExpiresAt = clock.instant().plus(refreshTtl);
        refreshTokens.save(RefreshToken.issue(user.id(), sha256(rawRefreshToken), refreshExpiresAt));
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.id(), user.username().value(),
                user.fullName(), permissions.stream().map(PermissionCode::value).collect(Collectors.toUnmodifiableSet()));
        return new AuthSession(accessTokens.issue(user, permissions), rawRefreshToken, refreshExpiresAt,
                authenticatedUser);
    }

    private Optional<User> findByUsername(String raw) {
        try {
            return users.findByUsername(new Username(raw));
        } catch (InvalidInputException malformed) {
            return Optional.empty();
        }
    }

    private Optional<RefreshToken> findToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return Optional.empty();
        }
        return refreshTokens.findByTokenHash(sha256(rawRefreshToken));
    }

    private static String newRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
