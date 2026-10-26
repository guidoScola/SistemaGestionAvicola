package com.guidoscola.avicola.security.domain.model;

import com.guidoscola.avicola.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsernameTest {

    @Test
    void normalizesToTrimmedLowercase() {
        assertThat(new Username("  JPerez ").value()).isEqualTo("jperez");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "ab", "with space", "ñandu"})
    void rejectsInvalidValues(String raw) {
        assertThatThrownBy(() -> new Username(raw)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void rejectsMoreThan50Characters() {
        assertThatThrownBy(() -> new Username("a".repeat(51))).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new Username(null)).isInstanceOf(InvalidInputException.class);
    }
}
