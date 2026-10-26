package com.guidoscola.avicola.security.infrastructure.web;

import com.guidoscola.avicola.security.application.AuthSession;
import com.guidoscola.avicola.security.application.AuthenticatedUser;
import com.guidoscola.avicola.security.application.AuthenticationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthenticationService authentication;
    private final boolean secureCookie;

    AuthController(AuthenticationService authentication,
                   @Value("${avicola.security.cookie-secure:true}") boolean secureCookie) {
        this.authentication = authentication;
        this.secureCookie = secureCookie;
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    record SessionResponse(String accessToken, Instant expiresAt, AuthenticatedUser user) {
    }

    @PostMapping("/login")
    ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request) {
        return sessionResponse(authentication.login(request.username(), request.password()));
    }

    @PostMapping("/refresh")
    ResponseEntity<SessionResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
        return sessionResponse(authentication.refresh(token));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
        authentication.logout(token);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/me")
    AuthenticatedUser me(@AuthenticationPrincipal Jwt jwt) {
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        return new AuthenticatedUser(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("username"),
                jwt.getClaimAsString("fullName"), permissions == null ? java.util.Set.of() : new HashSet<>(permissions));
    }

    private ResponseEntity<SessionResponse> sessionResponse(AuthSession session) {
        Duration maxAge = Duration.between(Instant.now(), session.refreshExpiresAt());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(), maxAge).toString())
                .body(new SessionResponse(session.accessToken().value(), session.accessToken().expiresAt(),
                        session.user()));
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
