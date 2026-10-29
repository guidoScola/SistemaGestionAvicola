package com.guidoscola.avicola.security.domain.model;

import com.guidoscola.avicola.shared.domain.InvalidInputException;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Role {

    private final UUID id;
    private String name;
    private Set<PermissionCode> permissions;

    public Role(UUID id, String name, Set<PermissionCode> permissions) {
        this.id = Objects.requireNonNull(id);
        this.name = requireName(name);
        this.permissions = Set.copyOf(permissions);
    }

    public static Role create(String name, Set<PermissionCode> permissions) {
        return new Role(UUID.randomUUID(), name, permissions);
    }

    public void update(String name, Set<PermissionCode> permissions) {
        this.name = requireName(name);
        this.permissions = Set.copyOf(permissions);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 50) {
            throw new InvalidInputException("Role name is required and must be at most 50 characters");
        }
        return name.trim();
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Set<PermissionCode> permissions() {
        return permissions;
    }
}
