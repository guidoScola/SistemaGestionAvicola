package com.guidoscola.avicola.security.infrastructure.web;

import com.guidoscola.avicola.security.application.RoleCommand;
import com.guidoscola.avicola.security.domain.model.Role;
import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RolesApiIT extends ApiIntegrationTest {

    @Test
    void listingRequiresRolesView() throws Exception {
        roleManagement.create(superAdmin().id(), new RoleCommand("Vendedor", Set.of("USERS_VIEW")));

        mvc.perform(get("/api/roles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/roles").header(HttpHeaders.AUTHORIZATION, bearerWith("USERS_VIEW")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/roles").header(HttpHeaders.AUTHORIZATION, bearerWith("ROLES_VIEW")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Vendedor")))
                .andExpect(jsonPath("$[?(@.name == 'Vendedor')].permissionCodes[0]").value("USERS_VIEW"));
    }

    @Test
    void permissionCatalogListsCodesWithDescriptions() throws Exception {
        mvc.perform(get("/api/permissions").header(HttpHeaders.AUTHORIZATION, bearerWith("USERS_VIEW")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/permissions").header(HttpHeaders.AUTHORIZATION, bearerWith("ROLES_VIEW")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code",
                        hasItems("USERS_VIEW", "USERS_MANAGE", "ROLES_VIEW", "ROLES_MANAGE", "SUPPLIERS_VIEW",
                                "SUPPLIERS_MANAGE")))
                .andExpect(jsonPath("$[?(@.code == 'USERS_VIEW')].description").value("View users"));
    }

    @Test
    void createsRoleOnlyWithRolesManage() throws Exception {
        String body = """
                {"name": "Contador", "permissionCodes": ["USERS_VIEW", "ROLES_VIEW"]}
                """;

        mvc.perform(post("/api/roles").header(HttpHeaders.AUTHORIZATION, bearerWith("ROLES_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/roles").header(HttpHeaders.AUTHORIZATION,
                                bearerWith("ROLES_MANAGE", "USERS_VIEW", "ROLES_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Contador"))
                .andExpect(jsonPath("$.permissionCodes", containsInAnyOrder("USERS_VIEW", "ROLES_VIEW")));

        assertThat(roleManagement.list()).extracting(Role::name).contains("Contador");
    }

    @Test
    void rejectsUnknownPermissionDuplicatesAndMissingName() throws Exception {
        roleManagement.create(superAdmin().id(), new RoleCommand("Vendedor", Set.of()));
        String auth = bearerWith("ROLES_MANAGE");

        mvc.perform(post("/api/roles").header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Otro", "permissionCodes": ["INVOICES_ISSUE"]}
                                """))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/roles").header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "vendedor", "permissionCodes": []}
                                """))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/roles").header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesRole() throws Exception {
        Role role = roleManagement.create(superAdmin().id(), new RoleCommand("Vendedor", Set.of()));

        mvc.perform(put("/api/roles/{id}", role.id())
                        .header(HttpHeaders.AUTHORIZATION, bearerWith("ROLES_MANAGE", "USERS_VIEW"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name": "Ventas", "permissionCodes": ["USERS_VIEW"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ventas"))
                .andExpect(jsonPath("$.permissionCodes[0]").value("USERS_VIEW"));
    }

    @Test
    void roleManagerCannotWidenTheirOwnRole() throws Exception {
        User manager = createUser("gestor", "ROLES_MANAGE");
        UUID ownRole = manager.roleIds().iterator().next();
        String ownRoleName = roleManagement.get(ownRole).name();

        mvc.perform(put("/api/roles/{id}", ownRole).header(HttpHeaders.AUTHORIZATION, bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name": "%s", "permissionCodes": ["ROLES_MANAGE", "USERS_MANAGE"]}
                                """.formatted(ownRoleName)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You can only grant permissions you already hold"));

        assertThat(roleManagement.get(ownRole).permissions()).extracting(p -> p.value()).containsExactly("ROLES_MANAGE");
    }

    @Test
    void deletesUnusedRoleButNotOneInUse() throws Exception {
        Role unused = roleManagement.create(superAdmin().id(), new RoleCommand("Temporal", Set.of()));
        String auth = bearerWith("ROLES_MANAGE");
        Role inUse = roleManagement.list().stream().filter(r -> r.name().startsWith("role-")).findFirst().orElseThrow();

        mvc.perform(delete("/api/roles/{id}", unused.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/roles/{id}", inUse.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isConflict());

        assertThat(roleManagement.list()).extracting(Role::id).doesNotContain(unused.id()).contains(inUse.id());
    }
}
