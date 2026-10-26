package com.guidoscola.avicola.security.application;

import java.util.Set;
import java.util.UUID;

public record UpdateUserCommand(String fullName, Set<UUID> roleIds) {
}
