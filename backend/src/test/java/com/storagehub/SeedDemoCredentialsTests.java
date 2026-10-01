package com.storagehub;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Pins the demo credential documented in README and seeded by
 * V2__seed_demo (dev profile only): password {@code Demo1234!} for all five
 * demo users. If someone regenerates the seed hash without updating the test
 * (or vice versa), this fails loudly instead of demo logins silently breaking.
 * Pure unit test - no Spring context, no DB, runs everywhere.
 */
class SeedDemoCredentialsTests {

    private static final String SEEDED_HASH =
            "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu";

    @Test
    void seededHashEncodesDocumentedDemoPassword() {
        assertTrue(new BCryptPasswordEncoder().matches("Demo1234!", SEEDED_HASH),
                "V2__seed_demo hash no longer matches the documented password Demo1234!");
    }
}
