package com.guidoscola.avicola.security.infrastructure.persistence;

import com.guidoscola.avicola.security.domain.model.PermissionCode;
import com.guidoscola.avicola.security.domain.model.Role;
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
import java.util.stream.Collectors;

@Entity
@Table(name = "roles")
class RoleEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 50)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission_code")
    private Set<String> permissionCodes = new HashSet<>();

    protected RoleEntity() {
    }

    static RoleEntity fromDomain(Role role) {
        RoleEntity entity = new RoleEntity();
        entity.id = role.id();
        entity.name = role.name();
        entity.permissionCodes = role.permissions().stream()
                .map(PermissionCode::value)
                .collect(Collectors.toCollection(HashSet::new));
        return entity;
    }

    Role toDomain() {
        return new Role(id, name, permissionCodes.stream().map(PermissionCode::new).collect(Collectors.toSet()));
    }
}
