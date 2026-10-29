package com.guidoscola.avicola.security.application;

import com.guidoscola.avicola.security.application.port.PermissionCatalog;
import com.guidoscola.avicola.security.domain.model.Permission;
import com.guidoscola.avicola.security.domain.model.PermissionCode;
import com.guidoscola.avicola.security.domain.model.Role;
import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.security.domain.model.Username;
import com.guidoscola.avicola.shared.domain.ConflictException;
import com.guidoscola.avicola.shared.domain.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleManagementServiceTest {

    private InMemoryRoleRepository roles;
    private InMemoryUserRepository users;
    private RoleManagementService service;

    @BeforeEach
    void setUp() {
        roles = new InMemoryRoleRepository();
        users = new InMemoryUserRepository();
        PermissionCatalog catalog = () -> List.of(
                new Permission(new PermissionCode("ROLES_VIEW"), "View roles"),
                new Permission(new PermissionCode("USERS_VIEW"), "View users"));
        service = new RoleManagementService(roles, users, catalog);
    }

    @Test
    void createsRoleWithKnownPermissions() {
        Role role = service.create(new RoleCommand("Vendedor", Set.of("USERS_VIEW")));

        assertThat(roles.findById(role.id())).isPresent();
        assertThat(role.permissions()).containsExactly(new PermissionCode("USERS_VIEW"));
    }

    @Test
    void rejectsPermissionOutsideCatalog() {
        assertThatThrownBy(() -> service.create(new RoleCommand("Vendedor", Set.of("INVOICES_ISSUE"))))
                .isInstanceOf(InvalidInputException.class);
        assertThat(roles.findAll()).isEmpty();
    }

    @Test
    void rejectsDuplicatedNameIgnoringCase() {
        service.create(new RoleCommand("Vendedor", Set.of()));

        assertThatThrownBy(() -> service.create(new RoleCommand("VENDEDOR", Set.of())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateKeepsOwnNameWithoutConflict() {
        Role role = service.create(new RoleCommand("Vendedor", Set.of()));

        Role updated = service.update(role.id(), new RoleCommand("vendedor", Set.of("ROLES_VIEW")));

        assertThat(updated.name()).isEqualTo("vendedor");
        assertThat(updated.permissions()).containsExactly(new PermissionCode("ROLES_VIEW"));
    }

    @Test
    void deletesUnusedRole() {
        Role role = service.create(new RoleCommand("Vendedor", Set.of()));

        service.delete(role.id());

        assertThat(roles.findAll()).isEmpty();
    }

    @Test
    void refusesToDeleteRoleAssignedToUsers() {
        Role role = service.create(new RoleCommand("Vendedor", Set.of()));
        users.save(User.create(new Username("jperez"), "Juan", "hash", Set.of(role.id())));

        assertThatThrownBy(() -> service.delete(role.id())).isInstanceOf(ConflictException.class);
        assertThat(roles.findAll()).hasSize(1);
    }
}
