package com.guidoscola.avicola.security.infrastructure.bootstrap;

import com.guidoscola.avicola.security.application.UserManagementService;
import com.guidoscola.avicola.security.application.port.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the super administrator on startup when the system has no users yet.
 * Credentials come from configuration (ADMIN_USERNAME / ADMIN_PASSWORD), never from migrations.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    static final String SUPER_ADMIN_FULL_NAME = "Administrador";

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final UserManagementService userManagement;
    private final String username;
    private final String password;

    AdminBootstrap(UserRepository users, UserManagementService userManagement,
                   @Value("${avicola.bootstrap.admin.username:}") String username,
                   @Value("${avicola.bootstrap.admin.password:}") String password) {
        this.users = users;
        this.userManagement = userManagement;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!users.findAll().isEmpty()) {
            return;
        }
        if (username.isBlank() || password.isBlank()) {
            log.warn("No users exist and ADMIN_USERNAME / ADMIN_PASSWORD are not set: nobody can log in");
            return;
        }
        userManagement.createSuperAdmin(username, SUPER_ADMIN_FULL_NAME, password);
        log.info("Bootstrap super administrator '{}' created", username.trim().toLowerCase());
    }
}
