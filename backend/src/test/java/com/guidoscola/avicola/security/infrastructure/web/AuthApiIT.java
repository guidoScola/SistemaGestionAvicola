package com.guidoscola.avicola.security.infrastructure.web;

import com.guidoscola.avicola.security.application.CreateUserCommand;
import com.guidoscola.avicola.security.application.RoleCommand;
import com.guidoscola.avicola.security.application.RoleManagementService;
import com.guidoscola.avicola.security.application.UserManagementService;
import com.guidoscola.avicola.security.domain.model.Role;
import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthApiIT extends IntegrationTest {

    private static final String JUAN_CREDENTIALS = """
            {"username": "jperez", "password": "secret123"}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserManagementService userManagement;

    @Autowired
    private RoleManagementService roleManagement;

    private User juan;

    @BeforeEach
    void createUser() {
        User root = userManagement.createSuperAdmin("root", "Root", "root-secret-1");
        Role seller = roleManagement.create(root.id(), new RoleCommand("Vendedor", Set.of("USERS_VIEW", "ROLES_VIEW")));
        juan = userManagement.create(root.id(),
                new CreateUserCommand("jperez", "Juan Perez", "secret123", Set.of(seller.id())));
    }

    private MvcResult login() throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(JUAN_CREDENTIALS))
                .andExpect(status().isOk())
                .andReturn();
    }

    private static String accessToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.accessToken");
    }

    private static Cookie refreshCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    @Test
    void loginReturnsAccessTokenUserAndHttpOnlyRefreshCookie() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(JUAN_CREDENTIALS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.user.id").value(juan.id().toString()))
                .andExpect(jsonPath("$.user.username").value("jperez"))
                .andExpect(jsonPath("$.user.fullName").value("Juan Perez"))
                .andExpect(jsonPath("$.user.permissions", containsInAnyOrder("USERS_VIEW", "ROLES_VIEW")))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).startsWith("refresh_token=")
                .contains("HttpOnly", "SameSite=Strict", "Path=/api/auth", "Max-Age=");
    }

    @Test
    void loginWithWrongPasswordReturns401WithoutCookie() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "jperez", "password": "wrong-pass"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid credentials"))
                .andExpect(result -> assertThat(result.getResponse().getCookie("refresh_token")).isNull());
    }

    @Test
    void loginWithMissingFieldsReturns400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void meRequiresAValidAccessToken() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());

        String token = accessToken(login());

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jperez"))
                .andExpect(jsonPath("$.permissions", containsInAnyOrder("USERS_VIEW", "ROLES_VIEW")));
    }

    @Test
    void refreshRotatesTheCookieAndRejectsTheOldOne() throws Exception {
        Cookie first = refreshCookie(login());

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("jperez"))
                .andReturn();
        Cookie second = refreshCookie(refreshed);
        assertThat(second.getValue()).isNotEqualTo(first.getValue());

        mvc.perform(post("/api/auth/refresh").cookie(first)).andExpect(status().isUnauthorized());
        // Reusing the old token revoked every session, including the rotated one.
        mvc.perform(post("/api/auth/refresh").cookie(second)).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieReturns401() throws Exception {
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutClearsTheCookieAndInvalidatesTheRefreshToken() throws Exception {
        Cookie cookie = refreshCookie(login());

        MvcResult result = mvc.perform(post("/api/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("refresh_token=", "Max-Age=0");
        mvc.perform(post("/api/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
