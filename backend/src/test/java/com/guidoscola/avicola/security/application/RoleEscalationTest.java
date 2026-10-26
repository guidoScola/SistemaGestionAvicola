package com.guidoscola.avicola.security.application;

import com.guidoscola.avicola.security.application.port.PermissionCatalog;
import com.guidoscola.avicola.security.domain.model.Permission;
import com.guidoscola.avicola.security.domain.model.PermissionCode;
import com.guidoscola.avicola.security.domain.model.Role;
import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.security.domain.model.Username;
import com.guidoscola.avicola.shared.domain.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Review finding R1-001: a ROLES_MANAGE holder must not widen a role beyond their own permissions (S6 rule 3).
 */
class RoleEscalationTest {

    private InMemoryRoleRepository roles;
    private RoleManagementService service;
    private User superAdmin;
    private User roleManager;
    private Role managerRole;

    @BeforeEach
    void setUp() {
        roles = new InMemoryRoleRepository();
        InMemoryUserRepository users = new InMemoryUserRepository();
        PermissionCatalog catalog = () -> List.of(
                new Permission(new PermissionCode("ROLES_MANAGE"), "-"),
                new Permission(new PermissionCode("ROLES_VIEW"), "-"),
                new Permission(new PermissionCode("USERS_MANAGE"), "-"));
        service = new RoleManagementService(roles, users, catalog, new PermissionResolver(roles, catalog));

        superAdmin = users.save(User.createSuperAdmin(new Username("root"), "Root", "hash"));
        managerRole = service.create(superAdmin.id(), new RoleCommand("Gestor de roles", Set.of("ROLES_MANAGE", "ROLES_VIEW")));
        roleManager = users.save(User.create(new Username("gestor"), "Gestor", "hash", Set.of(managerRole.id())));
    }

    @Test
    void cannotCreateARoleWithPermissionsTheyLack() {
        assertThatThrownBy(() -> service.create(roleManager.id(), new RoleCommand("Todo", Set.of("USERS_MANAGE"))))
                .isInstanceOf(ForbiddenException.class);
        assertThat(roles.findByNameIgnoreCase("Todo")).isEmpty();
    }

    @Test
    void cannotWidenTheirOwnRole() {
        assertThatThrownBy(() -> service.update(roleManager.id(), managerRole.id(),
                new RoleCommand("Gestor de roles", Set.of("ROLES_MANAGE", "ROLES_VIEW", "USERS_MANAGE"))))
                .isInstanceOf(ForbiddenException.class);
        assertThat(roles.findById(managerRole.id()).orElseThrow().permissions())
                .extracting(PermissionCode::value).containsExactlyInAnyOrder("ROLES_MANAGE", "ROLES_VIEW");
    }

    @Test
    void canCreateAndEditRolesWithinTheirPermissions() {
        Role viewer = service.create(roleManager.id(), new RoleCommand("Lector", Set.of("ROLES_VIEW")));

        Role renamed = service.update(roleManager.id(), viewer.id(), new RoleCommand("Lectura", Set.of("ROLES_VIEW")));

        assertThat(renamed.name()).isEqualTo("Lectura");
    }

    @Test
    void keepingPermissionsTheRoleAlreadyHadIsNotAnEscalation() {
        Role wide = service.create(superAdmin.id(), new RoleCommand("Amplio", Set.of("USERS_MANAGE", "ROLES_VIEW")));

        Role renamed = service.update(roleManager.id(), wide.id(), new RoleCommand("Amplio 2", Set.of("USERS_MANAGE")));

        assertThat(renamed.permissions()).extracting(PermissionCode::value).containsExactly("USERS_MANAGE");
    }

    @Test
    void superAdminCanGrantAnyPermission() {
        Role all = service.create(superAdmin.id(),
                new RoleCommand("Todo", Set.of("ROLES_MANAGE", "ROLES_VIEW", "USERS_MANAGE")));

        assertThat(all.permissions()).hasSize(3);
    }
}
