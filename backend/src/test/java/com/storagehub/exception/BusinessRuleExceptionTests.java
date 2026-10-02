package com.storagehub.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * The code of BusinessRuleException must match the openapi Error.code pattern
 * ^[A-Z][A-Z0-9_]*$ - a violating code would silently ship a 409 envelope the
 * FE cannot route on, so it is refused at construction instead. Pure JUnit.
 */
class BusinessRuleExceptionTests {

    @Test
    void upperSnakeCodeIsAcceptedAndCarried() {
        BusinessRuleException exception = new BusinessRuleException("UNIT_UNAVAILABLE",
                "The unit is no longer available for this period.");

        assertThat(exception.getCode()).isEqualTo("UNIT_UNAVAILABLE");
        assertThat(exception.getMessage()).isEqualTo("The unit is no longer available for this period.");
    }

    @Test
    void invalidCodesAreRefusedAtConstruction() {
        for (String badCode : new String[] { null, "", "unit_unavailable", "Unit Unavailable",
                "SHIFT-CONFLICT", "SHIFT CONFLICT", "1NVALID" }) {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> new BusinessRuleException(badCode, "message"))
                    .withMessageContaining("^[A-Z][A-Z0-9_]*$");
        }
    }
}
