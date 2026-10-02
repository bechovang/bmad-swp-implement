package com.storagehub.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Pure unit tests of the pagination input record (AD-8): defaults 1/25 for
 * absent params, explicit values kept, bounds below 1 fail fast in the
 * constructor. Pure JUnit - no Spring context, no DB.
 */
class ListQueryTests {

    @Test
    void absentParamsDefaultToPageOnePageSizeTwentyFive() {
        assertThat(ListQuery.of((Integer) null, (Integer) null)).isEqualTo(new ListQuery(1, 25));
        assertThat(ListQuery.of((String) null, null)).isEqualTo(new ListQuery(1, 25));
        assertThat(ListQuery.of("", "  ")).isEqualTo(new ListQuery(1, 25));
    }

    @Test
    void explicitValuesAreKept() {
        assertThat(ListQuery.of(3, 50)).isEqualTo(new ListQuery(3, 50));
    }

    @Test
    void stringVariantParsesNumbersAndKeepsDefaults() {
        assertThat(ListQuery.of("2", null)).isEqualTo(new ListQuery(2, 25));
        assertThat(ListQuery.of(null, "40")).isEqualTo(new ListQuery(1, 40));
        assertThat(ListQuery.of(" 7 ", "10")).isEqualTo(new ListQuery(7, 10));
    }

    @Test
    void pageBelowOneFailsFast() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ListQuery.of(0, 25))
                .withMessageContaining("page");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new ListQuery(-1, 25))
                .withMessageContaining("page");
    }

    @Test
    void pageSizeBelowOneFailsFast() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ListQuery.of(1, 0))
                .withMessageContaining("pageSize");
    }

    @Test
    void nonNumericInputFailsFast() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ListQuery.of("abc", null))
                .withMessageContaining("page");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ListQuery.of("1", "x"))
                .withMessageContaining("pageSize");
    }
}
