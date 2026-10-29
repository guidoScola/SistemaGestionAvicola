package com.guidoscola.avicola.security.infrastructure;

import com.guidoscola.avicola.security.application.RoleManagementService;
import com.guidoscola.avicola.security.application.UserManagementService;
import com.guidoscola.avicola.security.application.port.PasswordHasher;
import com.guidoscola.avicola.security.application.port.PermissionCatalog;
import com.guidoscola.avicola.security.application.port.RefreshTokenStore;
import com.guidoscola.avicola.security.application.port.RoleRepository;
import com.guidoscola.avicola.security.application.port.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SecurityUseCasesConfig {

    @Bean
    UserManagementService userManagementService(UserRepository users, RoleRepository roles, PasswordHasher hasher,
                                                RefreshTokenStore refreshTokens) {
        return new UserManagementService(users, roles, hasher, refreshTokens);
    }

    @Bean
    RoleManagementService roleManagementService(RoleRepository roles, UserRepository users, PermissionCatalog catalog) {
        return new RoleManagementService(roles, users, catalog);
    }
}
