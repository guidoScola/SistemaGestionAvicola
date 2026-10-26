package com.guidoscola.avicola.security.infrastructure.web;

import com.guidoscola.avicola.security.application.AuthenticationFailedException;
import com.guidoscola.avicola.security.application.RoleCommand;
import com.guidoscola.avicola.security.domain.model.Role;
import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.security.domain.model.Username;
import com.guidoscola.avicola.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsersApiIT extends ApiIntegrationTest {

    @Test
    void listingRequiresAuthenticationAndUsersView() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearerWith("ROLES_VIEW")))
                .andExpect(status().isForbidden());

        User juan = createUser("jperez");

        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearerWith("USERS_VIEW")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItem("jperez")))
                .andExpect(jsonPath("$[?(@.username == 'jperez')].id").value(juan.id().toString()))
                .andExpect(jsonPath("$[?(@.username == 'jperez')].active").value(true))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void superAdminIsProtectedFromUsersWithoutSuperAdminRights() throws Exception {
        User root = superAdmin();
        String manager = bearerWith("USERS_VIEW", "USERS_MANAGE");

        mvc.perform(get("/api/users/{id}", root.id()).header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.superAdmin").value(true));
        mvc.perform(post("/api/users/{id}/deactivate", root.id()).header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/users/{id}/password", root.id()).header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"newPassword": "hijacked-1"}
                                """))
                .andExpect(status().isForbidden());

        assertThat(userManagement.get(root.id()).isActive()).isTrue();
        assertThat(authentication.login(root.username().value(), PASSWORD)).isNotNull();
    }

    @Test
    void managerCannotAssignRolesWithPermissionsTheyLack() throws Exception {
        Role everything = roleManagement.create(new RoleCommand("Todo", Set.of("USERS_MANAGE", "ROLES_MANAGE")));
        User manager = createUser("gestor", "USERS_MANAGE");

        mvc.perform(put("/api/users/{id}", manager.id()).header(HttpHeaders.AUTHORIZATION, bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"fullName": "Gestor", "roleIds": ["%s"]}
                                """.formatted(everything.id())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You can only assign roles whose permissions you already hold"));

        assertThat(userManagement.get(manager.id()).roleIds()).doesNotContain(everything.id());
    }

    @Test
    void getsOneUserOr404() throws Exception {
        User juan = createUser("jperez");
        String auth = bearerWith("USERS_VIEW");

        mvc.perform(get("/api/users/{id}", juan.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Name jperez"));
        mvc.perform(get("/api/users/{id}", UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingRequiresUsersManage() throws Exception {
        Role seller = roleManagement.create(new RoleCommand("Vendedor", Set.of()));
        String body = """
                {"username": "mgomez", "fullName": "Maria Gomez", "password": "secret123", "roleIds": ["%s"]}
                """.formatted(seller.id());

        mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearerWith("USERS_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        assertThat(userManagement.list()).extracting(u -> u.username().value()).doesNotContain("mgomez");

        mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearerWith("USERS_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.username").value("mgomez"))
                .andExpect(jsonPath("$.roleIds[0]").value(seller.id().toString()));

        assertThat(authentication.login("mgomez", "secret123").user().username()).isEqualTo("mgomez");
    }

    @Test
    void creatingRejectsDuplicatesShortPasswordsAndMissingFields() throws Exception {
        createUser("jperez");
        String auth = bearerWith("USERS_MANAGE");

        mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "JPEREZ", "fullName": "Otro", "password": "secret123", "roleIds": []}
                                """))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "nuevo", "fullName": "Nuevo", "password": "short", "roleIds": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Password must be at least 8 characters"));
        mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesFullNameAndRoles() throws Exception {
        User juan = createUser("jperez");
        Role seller = roleManagement.create(new RoleCommand("Vendedor", Set.of()));

        mvc.perform(put("/api/users/{id}", juan.id()).header(HttpHeaders.AUTHORIZATION, bearerWith("USERS_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "Juan P.", "roleIds": ["%s"]}
                                """.formatted(seller.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Juan P."))
                .andExpect(jsonPath("$.roleIds[0]").value(seller.id().toString()));
    }

    @Test
    void deactivatedUserCannotLogInUntilReactivated() throws Exception {
        User juan = createUser("jperez");
        String auth = bearerWith("USERS_MANAGE");

        mvc.perform(post("/api/users/{id}/deactivate", juan.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
        assertThatThrownBy(() -> authentication.login("jperez", PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class);

        mvc.perform(post("/api/users/{id}/activate", juan.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
        assertThat(authentication.login("jperez", PASSWORD)).isNotNull();
    }

    @Test
    void adminResetsPassword() throws Exception {
        User juan = createUser("jperez");

        mvc.perform(put("/api/users/{id}/password", juan.id())
                        .header(HttpHeaders.AUTHORIZATION, bearerWith("USERS_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"newPassword": "brand-new-1"}
                                """))
                .andExpect(status().isNoContent());

        assertThat(authentication.login("jperez", "brand-new-1")).isNotNull();
    }

    @Test
    void anyAuthenticatedUserChangesOwnPassword() throws Exception {
        User juan = createUser("jperez");
        String auth = bearer(juan);

        mvc.perform(put("/api/users/me/password").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"currentPassword": "wrong-pass", "newPassword": "brand-new-1"}
                                """))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/users/me/password").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"currentPassword": "secret123", "newPassword": "brand-new-1"}
                                """))
                .andExpect(status().isNoContent());

        assertThat(authentication.login("jperez", "brand-new-1")).isNotNull();
        assertThat(userManagement.get(juan.id()).username()).isEqualTo(new Username("jperez"));
    }
}
