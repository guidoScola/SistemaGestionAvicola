package com.guidoscola.avicola.security.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
class PermissionEntity {

    @Id
    @Column(length = 64)
    private String code;

    @Column(nullable = false, length = 200)
    private String description;

    protected PermissionEntity() {
    }

    String code() {
        return code;
    }

    String description() {
        return description;
    }
}
