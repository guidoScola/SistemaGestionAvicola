package com.guidoscola.avicola.security.domain.model;

import com.guidoscola.avicola.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermissionCodeTest {

    @Test
    void acceptsUpperSnakeCase() {
        assertThat(new PermissionCode("USERS_MANAGE").value()).isEqualTo("USERS_MANAGE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "users_manage", "USERS-MANAGE", "_USERS"})
    void rejectsInvalidCodes(String raw) {
        assertThatThrownBy(() -> new PermissionCode(raw)).isInstanceOf(InvalidInputException.class);
    }
}
