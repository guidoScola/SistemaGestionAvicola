package com.guidoscola.avicola.security.infrastructure.bootstrap;

import com.guidoscola.avicola.security.application.port.PasswordHasher;
import com.guidoscola.avicola.security.application.port.PermissionCatalog;
import com.guidoscola.avicola.security.application.port.RoleRepository;
import com.guidoscola.avicola.security.application.port.UserRepository;
import com.guidoscola.avicola.security.domain.model.Role;
import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.security.domain.model.Username;
import com.guidoscola.avicola.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "avicola.bootstrap.admin.username=Admin",
        "avicola.bootstrap.admin.password=admin-secret-1"
})
class AdminBootstrapIT extends IntegrationTest {

    @Autowired
    private AdminBootstrap bootstrap;

    @Autowired
    private UserRepository users;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private PermissionCatalog catalog;

    @Autowired
    private PasswordHasher hasher;

    @Test
    void createsAdminWithAllPermissionsWhenNoUsersExist() {
        bootstrap.run(null);

        User admin = users.findByUsername(new Username("admin")).orElseThrow();
        Role adminRole = roles.findById(admin.roleIds().iterator().next()).orElseThrow();

        assertThat(admin.isActive()).isTrue();
        assertThat(hasher.matches("admin-secret-1", admin.passwordHash())).isTrue();
        assertThat(adminRole.name()).isEqualTo("Administrador");
        assertThat(adminRole.permissions()).isEqualTo(catalog.all());
    }

    @Test
    void doesNothingWhenUsersAlreadyExist() {
        users.save(User.create(new Username("jperez"), "Juan", "hash", Set.of()));

        bootstrap.run(null);

        assertThat(users.findAll()).extracting(u -> u.username().value()).containsExactly("jperez");
        assertThat(roles.findAll()).isEmpty();
    }
}
