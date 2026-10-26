package com.guidoscola.avicola.security.infrastructure.persistence;

import com.guidoscola.avicola.security.application.port.PermissionCatalog;
import com.guidoscola.avicola.security.application.port.RoleRepository;
import com.guidoscola.avicola.security.application.port.UserRepository;
import com.guidoscola.avicola.security.domain.model.PermissionCode;
import com.guidoscola.avicola.security.domain.model.Role;
import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.security.domain.model.Username;
import com.guidoscola.avicola.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityPersistenceIT extends IntegrationTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private PermissionCatalog catalog;

    @Test
    void catalogContainsSeededSecurityPermissions() {
        assertThat(catalog.all()).extracting(PermissionCode::value)
                .contains("USERS_VIEW", "USERS_MANAGE", "ROLES_VIEW", "ROLES_MANAGE");
    }

    @Test
    void persistsRoleWithPermissions() {
        Role role = roles.save(Role.create("Vendedor", Set.of(new PermissionCode("USERS_VIEW"))));

        Role found = roles.findByNameIgnoreCase("VENDEDOR").orElseThrow();

        assertThat(found.id()).isEqualTo(role.id());
        assertThat(found.permissions()).containsExactly(new PermissionCode("USERS_VIEW"));
    }

    @Test
    void updatesRolePermissions() {
        Role role = roles.save(Role.create("Vendedor", Set.of(new PermissionCode("USERS_VIEW"))));
        role.update("Ventas", Set.of(new PermissionCode("ROLES_VIEW")));
        roles.save(role);

        Role found = roles.findById(role.id()).orElseThrow();

        assertThat(found.name()).isEqualTo("Ventas");
        assertThat(found.permissions()).containsExactly(new PermissionCode("ROLES_VIEW"));
    }

    @Test
    void persistsUserWithRolesAndChecksRoleUsage() {
        Role role = roles.save(Role.create("Vendedor", Set.of()));
        Role unused = roles.save(Role.create("Contador", Set.of()));
        User user = users.save(User.create(new Username("jperez"), "Juan Perez", "hash", Set.of(role.id())));

        User found = users.findByUsername(new Username("jperez")).orElseThrow();

        assertThat(found.id()).isEqualTo(user.id());
        assertThat(found.fullName()).isEqualTo("Juan Perez");
        assertThat(found.passwordHash()).isEqualTo("hash");
        assertThat(found.isActive()).isTrue();
        assertThat(found.roleIds()).containsExactly(role.id());
        assertThat(users.existsByRoleId(role.id())).isTrue();
        assertThat(users.existsByRoleId(unused.id())).isFalse();
    }

    @Test
    void findsRolesByIdsAndDeletes() {
        Role a = roles.save(Role.create("A", Set.of()));
        Role b = roles.save(Role.create("B", Set.of()));

        assertThat(roles.findAllById(List.of(a.id(), b.id()))).hasSize(2);

        roles.delete(a);

        assertThat(roles.findAll()).extracting(Role::name).containsExactly("B");
    }
}
