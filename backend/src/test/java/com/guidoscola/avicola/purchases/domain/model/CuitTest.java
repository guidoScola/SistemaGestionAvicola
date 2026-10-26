package com.guidoscola.avicola.purchases.domain.model;

import com.guidoscola.avicola.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuitTest {

    @ParameterizedTest
    @ValueSource(strings = {"20-12345678-6", "20123456786", " 20 12345678 6 "})
    void acceptsValidCuitWithOrWithoutSeparators(String raw) {
        assertThat(new Cuit(raw).value()).isEqualTo("20123456786");
    }

    @Test
    void formatsWithDashes() {
        assertThat(new Cuit("30712345671").formatted()).isEqualTo("30-71234567-1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"20-12345678-5", "2012345678", "201234567861", "20-1234567A-6", ""})
    void rejectsInvalidCuit(String raw) {
        assertThatThrownBy(() -> new Cuit(raw)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new Cuit(null)).isInstanceOf(InvalidInputException.class);
    }
}
