package com.guidoscola.avicola.security.application;

import com.guidoscola.avicola.security.application.port.PermissionCatalog;
import com.guidoscola.avicola.security.application.port.RoleRepository;
import com.guidoscola.avicola.security.application.port.UserRepository;
import com.guidoscola.avicola.security.domain.model.Permission;
import com.guidoscola.avicola.security.domain.model.PermissionCode;
import com.guidoscola.avicola.security.domain.model.Role;
import com.guidoscola.avicola.shared.domain.ConflictException;
import com.guidoscola.avicola.shared.domain.InvalidInputException;
import com.guidoscola.avicola.shared.domain.NotFoundException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RoleManagementService {

    private final RoleRepository roles;
    private final UserRepository users;
    private final PermissionCatalog catalog;

    public RoleManagementService(RoleRepository roles, UserRepository users, PermissionCatalog catalog) {
        this.roles = roles;
        this.users = users;
        this.catalog = catalog;
    }

    public Role create(RoleCommand command) {
        Set<PermissionCode> permissions = requireKnownPermissions(command.permissionCodes());
        Role role = Role.create(command.name(), permissions);
        requireUniqueName(role.name(), null);
        return roles.save(role);
    }

    public Role update(UUID id, RoleCommand command) {
        Role role = get(id);
        Set<PermissionCode> permissions = requireKnownPermissions(command.permissionCodes());
        role.update(command.name(), permissions);
        requireUniqueName(role.name(), id);
        return roles.save(role);
    }

    public void delete(UUID id) {
        Role role = get(id);
        if (users.existsByRoleId(id)) {
            throw new ConflictException("Role is assigned to users and cannot be deleted");
        }
        roles.delete(role);
    }

    public Role get(UUID id) {
        return roles.findById(id).orElseThrow(() -> new NotFoundException("Role not found: " + id));
    }

    public List<Role> list() {
        return roles.findAll();
    }

    public List<Permission> permissionCatalog() {
        return catalog.list();
    }

    private void requireUniqueName(String name, UUID ownId) {
        roles.findByNameIgnoreCase(name)
                .filter(existing -> !existing.id().equals(ownId))
                .ifPresent(existing -> {
                    throw new ConflictException("Role name already exists: " + name);
                });
    }

    private Set<PermissionCode> requireKnownPermissions(Set<String> codes) {
        Set<PermissionCode> requested = codes == null ? Set.of()
                : codes.stream().map(PermissionCode::new).collect(Collectors.toUnmodifiableSet());
        if (!catalog.all().containsAll(requested)) {
            throw new InvalidInputException("One or more permissions do not exist");
        }
        return requested;
    }
}
