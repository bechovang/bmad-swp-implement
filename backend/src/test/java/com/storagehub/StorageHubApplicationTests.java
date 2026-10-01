package com.storagehub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: the Spring context loads against a local MySQL with the dev profile
 * (V1 schema + V2 demo seed). Requires the env vars from README ("Backend env vars")
 * and a running MySQL - it catches wiring/config breakage early (story 1.1).
 * Skips (does not fail) when DB_URL is unset, e.g. on a laptop without MySQL.
 */
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class StorageHubApplicationTests {

    @Test
    void contextLoads() {
    }

}
