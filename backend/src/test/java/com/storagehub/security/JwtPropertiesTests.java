package com.storagehub.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fail-fast binding rules of app.jwt.* (AD-5): a secret shorter than 32
 * characters or a non-positive TTL must kill startup at binding time, not
 * surface later as a weak signature. Pure unit test - the record's compact
 * constructor is exactly what Boot invokes while binding.
 */
class JwtPropertiesTests {

    @Test
    void bindsSecretAndTtl() {
        JwtProperties properties = new JwtProperties("0123456789abcdef0123456789abcdef", 12);

        assertThat(properties.secret()).isEqualTo("0123456789abcdef0123456789abcdef");
        assertThat(properties.ttlHours()).isEqualTo(12);
    }

    @Test
    void secretShorterThan32CharactersFailsFast() {
        assertThatThrownBy(() -> new JwtProperties("too-short", 24))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void nullSecretFailsFast() {
        assertThatThrownBy(() -> new JwtProperties(null, 24))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void nonPositiveTtlFailsFast() {
        assertThatThrownBy(() -> new JwtProperties("0123456789abcdef0123456789abcdef", 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ttl-hours");
    }
}
