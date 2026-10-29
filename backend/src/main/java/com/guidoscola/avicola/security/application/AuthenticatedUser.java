package com.guidoscola.avicola.security.application;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String username, String fullName, Set<String> permissions) {
}
