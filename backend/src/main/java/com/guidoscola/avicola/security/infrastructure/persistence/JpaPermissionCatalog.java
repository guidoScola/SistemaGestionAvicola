package com.guidoscola.avicola.security.infrastructure.persistence;

import com.guidoscola.avicola.security.application.port.PermissionCatalog;
import com.guidoscola.avicola.security.domain.model.PermissionCode;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.stream.Collectors;

@Repository
class JpaPermissionCatalog implements PermissionCatalog {

    private final PermissionJpaRepository jpa;

    JpaPermissionCatalog(PermissionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Set<PermissionCode> all() {
        return jpa.findAll().stream()
                .map(permission -> new PermissionCode(permission.code()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
