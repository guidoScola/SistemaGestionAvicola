package com.guidoscola.avicola.security.domain.model;

import com.guidoscola.avicola.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleTest {

    @Test
    void holdsNameAndPermissions() {
        Role role = Role.create("Vendedor", Set.of(new PermissionCode("USERS_VIEW")));

        assertThat(role.name()).isEqualTo("Vendedor");
        assertThat(role.permissions()).containsExactly(new PermissionCode("USERS_VIEW"));
    }

    @Test
    void requiresName() {
        assertThatThrownBy(() -> Role.create("  ", Set.of())).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void rejectsNameLongerThan50() {
        assertThatThrownBy(() -> Role.create("x".repeat(51), Set.of())).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void updateReplacesNameAndPermissions() {
        Role role = Role.create("Vendedor", Set.of(new PermissionCode("USERS_VIEW")));

        role.update("Ventas", Set.of(new PermissionCode("ROLES_VIEW")));

        assertThat(role.name()).isEqualTo("Ventas");
        assertThat(role.permissions()).containsExactly(new PermissionCode("ROLES_VIEW"));
    }
}
