package com.guidoscola.avicola.support;

import com.guidoscola.avicola.security.application.AuthenticationService;
import com.guidoscola.avicola.security.application.CreateUserCommand;
import com.guidoscola.avicola.security.application.RoleCommand;
import com.guidoscola.avicola.security.application.RoleManagementService;
import com.guidoscola.avicola.security.application.UserManagementService;
import com.guidoscola.avicola.security.domain.model.Role;
import com.guidoscola.avicola.security.domain.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

/**
 * Integration test base for the HTTP API with helpers to obtain access tokens with specific permissions.
 */
@AutoConfigureMockMvc
public abstract class ApiIntegrationTest extends IntegrationTest {

    protected static final String PASSWORD = "secret123";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected UserManagementService userManagement;

    @Autowired
    protected RoleManagementService roleManagement;

    @Autowired
    protected AuthenticationService authentication;

    private User superAdmin;

    /** The super administrator, created on first use; it performs the setup writes of each test. */
    protected User superAdmin() {
        if (superAdmin == null) {
            superAdmin = userManagement.createSuperAdmin("root" + UUID.randomUUID().toString().substring(0, 6),
                    "Root", PASSWORD);
        }
        return superAdmin;
    }

    protected User createUser(String username, String... permissions) {
        Role role = roleManagement.create(new RoleCommand("role-" + UUID.randomUUID(), Set.of(permissions)));
        return userManagement.create(superAdmin().id(),
                new CreateUserCommand(username, "Name " + username, PASSWORD, Set.of(role.id())));
    }

    protected String bearer(User user) {
        return "Bearer " + authentication.login(user.username().value(), PASSWORD).accessToken().value();
    }

    /** Creates a user holding exactly the given permissions and returns an Authorization header value. */
    protected String bearerWith(String... permissions) {
        return bearer(createUser("u" + UUID.randomUUID().toString().substring(0, 8), permissions));
    }
}
