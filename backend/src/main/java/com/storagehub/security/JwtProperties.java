package com.storagehub.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * app.jwt.* (application.yml). Binds the HS256 secret (env JWT_SECRET only -
 * never committed, AD-5) and the token TTL. Fail-fast happens in the compact
 * constructor, i.e. at binding time during startup: a missing JWT_SECRET env
 * var already kills the boot at the ${JWT_SECRET} placeholder, and a
 * configured-but-shorter-than-32-characters secret is rejected here (HS256
 * needs a >= 256-bit key).
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, @DefaultValue("24") int ttlHours) {

    /** TTL applied to every issued token (AD-5: 24h, no refresh). */
    public static final int DEFAULT_TTL_HOURS = 24;

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret (env JWT_SECRET) must be at least "
                    + "32 characters for HS256 - set a random string of 32+ characters");
        }
        if (ttlHours <= 0) {
            throw new IllegalStateException("app.jwt.ttl-hours must be positive (default "
                    + DEFAULT_TTL_HOURS + ")");
        }
    }
}
