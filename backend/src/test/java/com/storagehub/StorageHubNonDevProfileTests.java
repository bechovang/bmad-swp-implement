package com.storagehub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Matrix row "BE boot non-dev": NO profile active -> Flyway applies the
 * migration location only (V1 + V3 since story 1.3), the dev seed (V2, in
 * the dev-only location) never runs. Uses a dedicated schema
 * (storagehub_nondev_test, auto-created) so it cannot poison the dev DB.
 * Skips (does not fail) when DB_URL is unset, like StorageHubApplicationTests.
 * app.jwt.secret: the full context binds JwtProperties from 1.3 on - the
 * test value stands in for the JWT_SECRET env var (fail-fast secret rule).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=${DB_URL_NONDEV:jdbc:mysql://localhost:3306/storagehub_nondev_test?createDatabaseIfNotExist=true}",
        "app.jwt.secret=nondev-test-secret-0123456789-0123456789"})
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class StorageHubNonDevProfileTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void seedNeverRunsOutsideDevProfile() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> versions = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history ORDER BY installed_rank", String.class);
        assertEquals(List.of("1", "3"), versions,
                "non-dev boot must apply V1 + V3 only - the V2 demo seed leaked in");

        Integer users = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertEquals(Integer.valueOf(0), users, "users must be empty without the dev seed");
    }
}
