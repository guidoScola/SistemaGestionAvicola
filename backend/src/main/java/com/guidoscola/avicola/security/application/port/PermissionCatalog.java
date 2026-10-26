package com.guidoscola.avicola.security.application.port;

import com.guidoscola.avicola.security.domain.model.PermissionCode;

import java.util.Set;

@FunctionalInterface
public interface PermissionCatalog {

    Set<PermissionCode> all();
}
