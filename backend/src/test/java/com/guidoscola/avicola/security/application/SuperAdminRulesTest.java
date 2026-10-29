package com.guidoscola.avicola.security.application;

import com.guidoscola.avicola.security.domain.model.Permission;
import com.guidoscola.avicola.security.domain.model.PermissionCode;
import com.guidoscola.avicola.security.domain.model.Role;
import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.shared.domain.ConflictException;
import com.guidoscola.avicola.shared.domain.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rules agreed on 2026-10-26: protected super administrator and no privilege escalation through role assignment.
 */
class SuperAdminRulesTest {

    private InMemoryUserRepository users;
    private UserManagementService service;
    private User superAdmin;
    private User manager;
    private Role userManagerRole;
    private Role everythingRole;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        InMemoryRoleRepository roles = new InMemoryRoleRepository();
        var catalog = (com.guidoscola.avicola.security.application.port.PermissionCatalog) () -> List.of(
                new Permission(new PermissionCode("USERS_VIEW"), "-"),
                new Permission(new PermissionCode("USERS_MANAGE"), "-"),
                new Permission(new PermissionCode("ROLES_MANAGE"), "-"));
        service = new UserManagementService(users, roles, new FakePasswordHasher(), new InMemoryRefreshTokenStore(),
                new PermissionResolver(roles, catalog));

        superAdmin = service.createSuperAdmin("admin", "Admin", "admin-pass-1");
        userManagerRole = roles.save(Role.create("Gestor", Set.of(
                new PermissionCode("USERS_VIEW"), new PermissionCode("USERS_MANAGE"))));
        everythingRole = roles.save(Role.create("Todo", Set.of(
                new PermissionCode("USERS_VIEW"), new PermissionCode("USERS_MANAGE"), new PermissionCode("ROLES_MANAGE"))));
        manager = service.create(superAdmin.id(),
                new CreateUserCommand("gestor", "Gestor", "secret123", Set.of(userManagerRole.id())));
    }

    @Test
    void superAdminIsCreatedActiveAndFlagged() {
        assertThat(superAdmin.isSuperAdmin()).isTrue();
        assertThat(superAdmin.isActive()).isTrue();
        assertThat(manager.isSuperAdmin()).isFalse();
    }

    @Test
    void nonSuperAdminCannotTouchTheSuperAdmin() {
        assertThatThrownBy(() -> service.update(manager.id(), superAdmin.id(), new UpdateUserCommand("X", Set.of())))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.deactivate(manager.id(), superAdmin.id()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.activate(manager.id(), superAdmin.id()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.resetPassword(manager.id(), superAdmin.id(), "hijacked-1"))
                .isInstanceOf(ForbiddenException.class);

        User unchanged = users.findById(superAdmin.id()).orElseThrow();
        assertThat(unchanged.fullName()).isEqualTo("Admin");
        assertThat(unchanged.isActive()).isTrue();
        assertThat(unchanged.passwordHash()).isEqualTo("hashed:admin-pass-1");
    }

    @Test
    void nonSuperAdminCanOnlyAssignRolesWithinTheirOwnPermissions() {
        assertThatThrownBy(() -> service.create(manager.id(),
                new CreateUserCommand("nuevo", "Nuevo", "secret123", Set.of(everythingRole.id()))))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.update(manager.id(), manager.id(),
                new UpdateUserCommand("Gestor", Set.of(everythingRole.id()))))
                .isInstanceOf(ForbiddenException.class);

        assertThat(users.findByUsername(new com.guidoscola.avicola.security.domain.model.Username("nuevo"))).isEmpty();
        assertThat(users.findById(manager.id()).orElseThrow().roleIds()).containsExactly(userManagerRole.id());

        User allowed = service.create(manager.id(),
                new CreateUserCommand("ayudante", "Ayudante", "secret123", Set.of(userManagerRole.id())));
        assertThat(allowed.roleIds()).containsExactly(userManagerRole.id());
    }

    @Test
    void keepingAnAlreadyAssignedRoleIsNotAnEscalation() {
        User target = service.create(superAdmin.id(),
                new CreateUserCommand("jefe", "Jefe", "secret123", Set.of(everythingRole.id())));

        User renamed = service.update(manager.id(), target.id(),
                new UpdateUserCommand("Jefe de ventas", Set.of(everythingRole.id())));

        assertThat(renamed.fullName()).isEqualTo("Jefe de ventas");
    }

    @Test
    void nonSuperAdminCannotResetPasswordOfSomeoneWithMorePermissions() {
        User boss = service.create(superAdmin.id(),
                new CreateUserCommand("jefe", "Jefe", "secret123", Set.of(everythingRole.id())));

        assertThatThrownBy(() -> service.resetPassword(manager.id(), boss.id(), "hijacked-1"))
                .isInstanceOf(ForbiddenException.class);
        assertThat(users.findById(boss.id()).orElseThrow().passwordHash()).isEqualTo("hashed:secret123");
    }

    @Test
    void superAdminCanAssignAnyRole() {
        User created = service.create(superAdmin.id(),
                new CreateUserCommand("jefe", "Jefe", "secret123", Set.of(everythingRole.id())));

        assertThat(created.roleIds()).containsExactly(everythingRole.id());
    }

    @Test
    void lastActiveSuperAdminCannotBeDeactivated() {
        assertThatThrownBy(() -> service.deactivate(superAdmin.id(), superAdmin.id()))
                .isInstanceOf(ConflictException.class);
        assertThat(users.findById(superAdmin.id()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void superAdminGetsEveryPermissionRegardlessOfRoles() {
        PermissionResolver resolver = new PermissionResolver(new InMemoryRoleRepository(), () -> List.of(
                new Permission(new PermissionCode("USERS_VIEW"), "-"),
                new Permission(new PermissionCode("INVOICES_ISSUE"), "-")));

        assertThat(resolver.effectivePermissions(superAdmin)).extracting(PermissionCode::value)
                .containsExactlyInAnyOrder("USERS_VIEW", "INVOICES_ISSUE");
    }
}
