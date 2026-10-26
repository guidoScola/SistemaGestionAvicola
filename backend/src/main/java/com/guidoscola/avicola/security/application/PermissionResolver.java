package com.guidoscola.avicola.security.application;

import com.guidoscola.avicola.security.application.port.PermissionCatalog;
import com.guidoscola.avicola.security.application.port.RoleRepository;
import com.guidoscola.avicola.security.domain.model.PermissionCode;
import com.guidoscola.avicola.security.domain.model.Role;
import com.guidoscola.avicola.security.domain.model.User;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Single source of truth for what a user is allowed to do.
 */
public class PermissionResolver {

    private final RoleRepository roles;
    private final PermissionCatalog catalog;

    public PermissionResolver(RoleRepository roles, PermissionCatalog catalog) {
        this.roles = roles;
        this.catalog = catalog;
    }

    /** Super admins hold every permission in the catalog; everyone else holds the union of their roles. */
    public Set<PermissionCode> effectivePermissions(User user) {
        if (user.isSuperAdmin()) {
            return catalog.all();
        }
        return permissionsOfRoles(user.roleIds());
    }

    public Set<PermissionCode> permissionsOfRoles(Collection<UUID> roleIds) {
        return roles.findAllById(roleIds).stream()
                .map(Role::permissions)
                .flatMap(Set::stream)
                .collect(Collectors.toUnmodifiableSet());
    }
}
