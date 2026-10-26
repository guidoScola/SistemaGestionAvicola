package com.guidoscola.avicola.security.infrastructure.persistence;

import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.security.domain.model.Username;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "super_admin", nullable = false)
    private boolean superAdmin;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_id")
    private Set<UUID> roleIds = new HashSet<>();

    protected UserEntity() {
    }

    static UserEntity fromDomain(User user) {
        UserEntity entity = new UserEntity();
        entity.id = user.id();
        entity.username = user.username().value();
        entity.fullName = user.fullName();
        entity.passwordHash = user.passwordHash();
        entity.active = user.isActive();
        entity.superAdmin = user.isSuperAdmin();
        entity.roleIds = new HashSet<>(user.roleIds());
        return entity;
    }

    User toDomain() {
        return new User(id, new Username(username), fullName, passwordHash, active, superAdmin, roleIds);
    }
}
