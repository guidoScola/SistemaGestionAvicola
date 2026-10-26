package com.guidoscola.avicola.security.application;

import java.util.Set;
import java.util.UUID;

public record CreateUserCommand(String username, String fullName, String password, Set<UUID> roleIds) {
}
