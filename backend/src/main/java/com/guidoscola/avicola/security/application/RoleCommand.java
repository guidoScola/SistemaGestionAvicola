package com.guidoscola.avicola.security.application;

import java.util.Set;

public record RoleCommand(String name, Set<String> permissionCodes) {
}
