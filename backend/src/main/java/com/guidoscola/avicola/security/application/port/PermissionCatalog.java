package com.guidoscola.avicola.security.application.port;

import com.guidoscola.avicola.security.domain.model.Permission;
import com.guidoscola.avicola.security.domain.model.PermissionCode;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@FunctionalInterface
public interface PermissionCatalog {

    /** Every permission the system knows, ordered by code. */
    List<Permission> list();

    default Set<PermissionCode> all() {
        return list().stream().map(Permission::code).collect(Collectors.toUnmodifiableSet());
    }
}
