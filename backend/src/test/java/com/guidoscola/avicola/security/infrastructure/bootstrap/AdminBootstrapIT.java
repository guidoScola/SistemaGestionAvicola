package com.guidoscola.avicola.security.infrastructure.bootstrap;

import com.guidoscola.avicola.security.application.PermissionResolver;
import com.guidoscola.avicola.security.application.port.PasswordHasher;
import com.guidoscola.avicola.security.application.port.PermissionCatalog;
import com.guidoscola.avicola.security.application.port.RoleRepository;
import com.guidoscola.avicola.security.application.port.UserRepository;
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

    @Autowired
    private PermissionResolver permissions;

    @Test
    void createsSuperAdminWithAllPermissionsWhenNoUsersExist() {
        bootstrap.run(null);

        User admin = users.findByUsername(new Username("admin")).orElseThrow();

        assertThat(admin.isActive()).isTrue();
        assertThat(admin.isSuperAdmin()).isTrue();
        assertThat(admin.roleIds()).isEmpty();
        assertThat(hasher.matches("admin-secret-1", admin.passwordHash())).isTrue();
        assertThat(permissions.effectivePermissions(admin)).isEqualTo(catalog.all());
        assertThat(roles.findAll()).isEmpty();
    }

    @Test
    void doesNothingWhenUsersAlreadyExist() {
        users.save(User.create(new Username("jperez"), "Juan", "hash", Set.of()));

        bootstrap.run(null);

        assertThat(users.findAll()).extracting(u -> u.username().value()).containsExactly("jperez");
        assertThat(roles.findAll()).isEmpty();
    }
}
