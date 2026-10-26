package com.guidoscola.avicola.security.infrastructure.persistence;

import com.guidoscola.avicola.security.application.port.RoleRepository;
import com.guidoscola.avicola.security.domain.model.Role;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaRoleRepository implements RoleRepository {

    private final RoleJpaRepository jpa;

    JpaRoleRepository(RoleJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Role save(Role role) {
        return jpa.save(RoleEntity.fromDomain(role)).toDomain();
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return jpa.findById(id).map(RoleEntity::toDomain);
    }

    @Override
    public Optional<Role> findByNameIgnoreCase(String name) {
        return jpa.findByNameIgnoreCase(name).map(RoleEntity::toDomain);
    }

    @Override
    public List<Role> findAll() {
        return jpa.findAll(Sort.by("name")).stream().map(RoleEntity::toDomain).toList();
    }

    @Override
    public List<Role> findAllById(Collection<UUID> ids) {
        return jpa.findAllById(ids).stream().map(RoleEntity::toDomain).toList();
    }

    @Override
    public void delete(Role role) {
        jpa.deleteById(role.id());
    }
}
