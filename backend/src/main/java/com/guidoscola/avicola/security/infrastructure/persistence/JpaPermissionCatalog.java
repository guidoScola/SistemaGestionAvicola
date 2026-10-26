package com.guidoscola.avicola.security.infrastructure.persistence;

import com.guidoscola.avicola.security.application.port.PermissionCatalog;
import com.guidoscola.avicola.security.domain.model.Permission;
import com.guidoscola.avicola.security.domain.model.PermissionCode;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class JpaPermissionCatalog implements PermissionCatalog {

    private final PermissionJpaRepository jpa;

    JpaPermissionCatalog(PermissionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Permission> list() {
        return jpa.findAll(Sort.by("code")).stream()
                .map(permission -> new Permission(new PermissionCode(permission.code()), permission.description()))
                .toList();
    }
}
