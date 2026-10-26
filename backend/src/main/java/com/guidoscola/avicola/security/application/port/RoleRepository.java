package com.guidoscola.avicola.security.application.port;

import com.guidoscola.avicola.security.domain.model.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {

    Role save(Role role);

    Optional<Role> findById(UUID id);

    Optional<Role> findByNameIgnoreCase(String name);

    List<Role> findAll();

    List<Role> findAllById(Collection<UUID> ids);

    void delete(Role role);
}
