package com.guidoscola.avicola.security.application;

import com.guidoscola.avicola.security.application.port.RoleRepository;
import com.guidoscola.avicola.security.domain.model.Role;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryRoleRepository implements RoleRepository {

    private final Map<UUID, Role> roles = new LinkedHashMap<>();

    @Override
    public Role save(Role role) {
        roles.put(role.id(), role);
        return role;
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return Optional.ofNullable(roles.get(id));
    }

    @Override
    public Optional<Role> findByNameIgnoreCase(String name) {
        return roles.values().stream().filter(r -> r.name().equalsIgnoreCase(name)).findFirst();
    }

    @Override
    public List<Role> findAll() {
        return List.copyOf(roles.values());
    }

    @Override
    public List<Role> findAllById(Collection<UUID> ids) {
        return roles.values().stream().filter(r -> ids.contains(r.id())).toList();
    }

    @Override
    public void delete(Role role) {
        roles.remove(role.id());
    }
}
