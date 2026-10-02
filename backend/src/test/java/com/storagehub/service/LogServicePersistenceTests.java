package com.storagehub.service;

import com.storagehub.entity.Action;
import com.storagehub.entity.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Real-MySQL persistence of LogService.append (guard pattern from story 1.1:
 * skips without DB_URL). Uses a dedicated schema (storagehub_log_test,
 * auto-created, Flyway V1 only - no dev seed) so the shared dev database is
 * never touched; every test is transactional and rolls back. Seeding a role
 * and a user satisfies the ActorID FK of activity_logs. Reading the row back
 * through plain JDBC verifies what was actually stored: the UPPER_SNAKE enum
 * strings, the from/to values and "" for an optional reason (Reason NOT NULL).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties =
        "spring.datasource.url=${DB_URL_LOGTEST:jdbc:mysql://localhost:3306/storagehub_log_test?createDatabaseIfNotExist=true}")
@Import(LogService.class)
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class LogServicePersistenceTests {

    private static final int TEST_ROLE_ID = 990;

    private static final long TEST_ACTOR_ID = 990_001L;

    @Autowired
    private LogService logService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedActorForTheForeignKey() {
        jdbcTemplate.update("INSERT INTO roles (RoleID, Name, Description) VALUES (?, ?, ?)",
                TEST_ROLE_ID, "STORY_1_2_TEST", "story 1.2 persistence test fixture role");
        jdbcTemplate.update(
                "INSERT INTO users (UserID, FullName, Email, PasswordHash, RoleID, Status) "
                        + "VALUES (?, ?, ?, ?, ?, 1)",
                TEST_ACTOR_ID, "Story 1.2 Test Actor", "story-1-2-test@storagehub.dev",
                "not-a-real-hash", TEST_ROLE_ID);
    }

    @Test
    void appendInsertsARowMatchingTheV1Columns() {
        logService.append(TEST_ACTOR_ID, EntityType.UNIT, 3L, Action.STATUS_CHANGE,
                "AVAILABLE", "MAINTENANCE", null);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT ActorID, EntityType, EntityID, Action, FromValue, ToValue, Reason "
                        + "FROM activity_logs WHERE ActorID = ?",
                TEST_ACTOR_ID);

        assertThat(row)
                .containsEntry("ActorID", TEST_ACTOR_ID)
                .containsEntry("EntityType", "UNIT")
                .containsEntry("EntityID", 3L)
                .containsEntry("Action", "STATUS_CHANGE")
                .containsEntry("FromValue", "AVAILABLE")
                .containsEntry("ToValue", "MAINTENANCE")
                .containsEntry("Reason", "");
    }

    @Test
    void enumsAreStoredAsUpperSnakeStrings() {
        logService.append(TEST_ACTOR_ID, EntityType.RESERVATION, 42L, Action.CONTRACT_SIGNED,
                null, "SIGNED", null);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT EntityType, Action, FromValue, ToValue, Reason "
                        + "FROM activity_logs WHERE ActorID = ?",
                TEST_ACTOR_ID);

        assertThat(row)
                .containsEntry("EntityType", "RESERVATION")
                .containsEntry("Action", "CONTRACT_SIGNED")
                .containsEntry("FromValue", null)
                .containsEntry("ToValue", "SIGNED")
                .containsEntry("Reason", "");
    }

    @Test
    void reasonRequiredRefusalInsertsNothing() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> logService.append(TEST_ACTOR_ID, EntityType.UNIT, 3L,
                        Action.FIX_STATUS, "MAINTENANCE", "RETIRED", " "))
                .withMessageContaining("FIX_STATUS");

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM activity_logs WHERE ActorID = ?", Integer.class, TEST_ACTOR_ID);
        assertThat(rows).isZero();
    }
}
