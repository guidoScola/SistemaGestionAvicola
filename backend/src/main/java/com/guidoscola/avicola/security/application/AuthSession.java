package com.guidoscola.avicola.security.application;

import java.time.Instant;

public record AuthSession(AccessToken accessToken, String refreshToken, Instant refreshExpiresAt,
                          AuthenticatedUser user) {
}
