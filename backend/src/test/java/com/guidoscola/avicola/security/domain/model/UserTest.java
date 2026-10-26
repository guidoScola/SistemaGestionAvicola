package com.guidoscola.avicola.security.domain.model;

import com.guidoscola.avicola.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private final UUID roleId = UUID.randomUUID();

    @Test
    void newUserIsActiveWithGivenRoles() {
        User user = User.create(new Username("jperez"), "Juan Perez", "hash", Set.of(roleId));

        assertThat(user.id()).isNotNull();
        assertThat(user.isActive()).isTrue();
        assertThat(user.roleIds()).containsExactly(roleId);
    }

    @Test
    void requiresFullName() {
        assertThatThrownBy(() -> User.create(new Username("jperez"), " ", "hash", Set.of()))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void canBeDeactivatedAndReactivated() {
        User user = User.create(new Username("jperez"), "Juan Perez", "hash", Set.of());

        user.deactivate();
        assertThat(user.isActive()).isFalse();

        user.activate();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void changingPasswordReplacesHash() {
        User user = User.create(new Username("jperez"), "Juan Perez", "old", Set.of());

        user.changePasswordHash("new");

        assertThat(user.passwordHash()).isEqualTo("new");
    }
}
