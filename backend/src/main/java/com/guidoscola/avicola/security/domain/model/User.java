package com.guidoscola.avicola.security.domain.model;

import com.guidoscola.avicola.shared.domain.InvalidInputException;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class User {

    private final UUID id;
    private final Username username;
    private String fullName;
    private String passwordHash;
    private boolean active;
    private final boolean superAdmin;
    private Set<UUID> roleIds;

    public User(UUID id, Username username, String fullName, String passwordHash, boolean active,
                boolean superAdmin, Set<UUID> roleIds) {
        this.id = Objects.requireNonNull(id);
        this.username = Objects.requireNonNull(username);
        this.fullName = requireFullName(fullName);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.active = active;
        this.superAdmin = superAdmin;
        this.roleIds = Set.copyOf(roleIds);
    }

    public static User create(Username username, String fullName, String passwordHash, Set<UUID> roleIds) {
        return new User(UUID.randomUUID(), username, fullName, passwordHash, true, false, roleIds);
    }

    /** The top-level administrator: holds every permission and can only be managed by another super admin. */
    public static User createSuperAdmin(Username username, String fullName, String passwordHash) {
        return new User(UUID.randomUUID(), username, fullName, passwordHash, true, true, Set.of());
    }

    public void update(String fullName, Set<UUID> roleIds) {
        this.fullName = requireFullName(fullName);
        this.roleIds = Set.copyOf(roleIds);
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private static String requireFullName(String fullName) {
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 100) {
            throw new InvalidInputException("Full name is required and must be at most 100 characters");
        }
        return fullName.trim();
    }

    public UUID id() {
        return id;
    }

    public Username username() {
        return username;
    }

    public String fullName() {
        return fullName;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public Set<UUID> roleIds() {
        return roleIds;
    }
}
