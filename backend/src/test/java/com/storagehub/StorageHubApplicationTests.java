package com.storagehub;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test: the Spring context loads against a local MySQL with the dev profile
 * (V1 schema + V3 actor-nullable + V2 demo seed). Requires the env vars from README
 * ("Backend env vars") and a running MySQL - it catches wiring/config breakage early
 * (story 1.1).
 * Story 1.2 locks the security chain end-to-end over real HTTP: the one public path
 * stays 200 and everything else answers the 401 envelope.
 * Story 1.3 drives the real auth endpoints over HTTP: seeded login succeeds and is
 * audited (LOGIN), every failure branch shares one generic message and is audited
 * (LOGIN_FAILED - unknown email with ActorID NULL), and an issued token actually
 * authenticates a request. app.jwt.secret stands in for the JWT_SECRET env var so
 * the full context can bind JwtProperties (fail-fast secret rule).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("dev")
@TestPropertySource(properties = "app.jwt.secret=app-test-secret-0123456789-0123456789")
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class StorageHubApplicationTests {

    /** Reason literal AuthService writes on known-user LOGIN_FAILED rows (cleanup discriminator). */
    private static final String LOGIN_FAILED_REASON = "Sign-in failed";

    private static final String UNKNOWN_EMAIL = "story-1-3-unknown@storagehub.dev";
    private static final String REGISTER_SUCCESS_EMAIL = "success-story-1-3@storagehub.dev";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Removes only the audit rows and users THIS test class appended (seed rows carry different Reasons). */
    @AfterEach
    void cleanUpAuditRowsCreatedByTheseTests() {
        jdbcTemplate.update("DELETE FROM activity_logs WHERE ActorID = 1 AND Action = 'LOGIN' AND Reason = ''");
        jdbcTemplate.update("DELETE FROM activity_logs WHERE ActorID = 1 AND Action = 'LOGIN_FAILED' AND Reason = ?",
                LOGIN_FAILED_REASON);
        jdbcTemplate.update("DELETE FROM activity_logs WHERE ActorID IS NULL AND Action = 'LOGIN_FAILED' "
                + "AND Reason LIKE ?", "%unknown email: " + UNKNOWN_EMAIL + "%");

        Integer registeredUserId = jdbcTemplate.query(
                "SELECT UserID FROM users WHERE Email = ?",
                rs -> rs.next() ? rs.getInt("UserID") : null,
                REGISTER_SUCCESS_EMAIL);
        if (registeredUserId != null) {
            jdbcTemplate.update("DELETE FROM activity_logs WHERE ActorID = ?", registeredUserId);
            jdbcTemplate.update("DELETE FROM users WHERE UserID = ?", registeredUserId);
        }
    }

    @Test
    void contextLoads() {
    }

    @Test
    void actuatorHealthStaysPublicOverRealHttp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void anyOtherPathAnswersThe401EnvelopeOverRealHttp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/anything", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(response.getBody()).contains("\"code\":\"UNAUTHENTICATED\"");
    }

    @Test
    void seededLoginSucceedsIssuesTokenAndIsAudited() {
        long loginRowsBefore = countAuditRows("ActorID = 1 AND Action = 'LOGIN'");

        ResponseEntity<String> response = login("lan@storagehub.dev", "Demo1234!");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = JsonPath.read(response.getBody(), "$.token");
        assertThat(token).isNotBlank();
        assertThat((Integer) JsonPath.read(response.getBody(), "$.user.id")).isEqualTo(1);
        assertThat(JsonPath.<String>read(response.getBody(), "$.user.fullName")).isEqualTo("Lan Nguyen");
        assertThat(JsonPath.<String>read(response.getBody(), "$.user.email")).isEqualTo("lan@storagehub.dev");
        assertThat(JsonPath.<String>read(response.getBody(), "$.user.role")).isEqualTo("CUSTOMER");
        assertThat(JsonPath.<String>read(response.getBody(), "$.user.phone")).isEqualTo("0901234567");

        assertThat(countAuditRows("ActorID = 1 AND Action = 'LOGIN'")).isEqualTo(loginRowsBefore + 1);
    }

    @Test
    void wrongPasswordFailsWithTheSharedMessageAndIsAudited() {
        // Same filter before and after: our rows carry the service's literal
        // reason, the seed's LOGIN_FAILED row carries a different one.
        long failedRowsBefore = countAuditRows(
                "ActorID = 1 AND Action = 'LOGIN_FAILED' AND Reason = '" + LOGIN_FAILED_REASON + "'");

        ResponseEntity<String> response = login("lan@storagehub.dev", "WrongPassword1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(JsonPath.<String>read(response.getBody(), "$.code")).isEqualTo("UNAUTHENTICATED");
        String message = JsonPath.read(response.getBody(), "$.message");

        assertThat(countAuditRows(
                "ActorID = 1 AND Action = 'LOGIN_FAILED' AND Reason = '" + LOGIN_FAILED_REASON + "'"))
                .isEqualTo(failedRowsBefore + 1);

        // Same branch behavior as the unknown-email row of the matrix (one shared message).
        ResponseEntity<String> unknownResponse = login(UNKNOWN_EMAIL, "Whatever123");
        assertThat(unknownResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(JsonPath.<String>read(unknownResponse.getBody(), "$.message")).isEqualTo(message);
    }

    @Test
    void unknownEmailIsAuditedWithNullActorAndTheAttemptedEmailInReason() {
        long nullActorRowsBefore = countAuditRows("ActorID IS NULL AND Action = 'LOGIN_FAILED'");

        ResponseEntity<String> response = login(UNKNOWN_EMAIL, "Whatever123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(JsonPath.<String>read(response.getBody(), "$.code")).isEqualTo("UNAUTHENTICATED");

        assertThat(countAuditRows("ActorID IS NULL AND Action = 'LOGIN_FAILED'")).isEqualTo(nullActorRowsBefore + 1);
        String reason = jdbcTemplate.queryForObject(
                "SELECT Reason FROM activity_logs WHERE ActorID IS NULL AND Action = 'LOGIN_FAILED' "
                        + "AND Reason LIKE '%unknown email: " + UNKNOWN_EMAIL + "%' ORDER BY LogID DESC LIMIT 1",
                String.class);
        assertThat(reason).contains(UNKNOWN_EMAIL);
    }

    @Test
    void issuedTokenAuthenticatesARequestThroughTheFilter() {
        String token = JsonPath.read(login("lan@storagehub.dev", "Demo1234!").getBody(), "$.token");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/anything", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        // Authenticated (filter accepted the token) - the path itself just does not exist.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(JsonPath.<String>read(response.getBody(), "$.code")).isEqualTo("NOT_FOUND");
    }

    @Test
    void registerDuplicateEmailAndCrossFieldFailuresAnswerThe400FieldErrorsEnvelope() {
        // Duplicate against the seeded account: 400 + fieldErrors[email], no row written.
        ResponseEntity<String> duplicate =
                register("lan@storagehub.dev", "MyPass123!", "MyPass123!", true);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.<String>read(duplicate.getBody(), "$.code")).isEqualTo("VALIDATION_FAILED");
        assertThat(JsonPath.<String>read(duplicate.getBody(), "$.fieldErrors[0].field")).isEqualTo("email");

        // Cross-field failures: both offending fields in one envelope, nothing written either.
        ResponseEntity<String> crossField =
                register("fresh-story-1-3@storagehub.dev", "MyPass123!", "MyPass456!", false);
        assertThat(crossField.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.<String>read(crossField.getBody(), "$.code")).isEqualTo("VALIDATION_FAILED");
        List<String> fields = JsonPath.read(crossField.getBody(), "$.fieldErrors[*].field");
        assertThat(fields).containsExactlyInAnyOrder("confirmPassword", "agreeToTerms");
    }

    @Test
    void registerSuccessfulPersistsUserAndAllowsSubsequentLogin() {
        ResponseEntity<String> response =
                register(REGISTER_SUCCESS_EMAIL, "ValidPass123!", "ValidPass123!", true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Integer id = JsonPath.read(response.getBody(), "$.id");
        assertThat(id).isNotNull().isPositive();
        assertThat(JsonPath.<String>read(response.getBody(), "$.email")).isEqualTo(REGISTER_SUCCESS_EMAIL);
        assertThat(JsonPath.<String>read(response.getBody(), "$.role")).isEqualTo("CUSTOMER");

        // Verify row in MySQL
        Integer dbCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE Email = ? AND Status = 1", Integer.class, REGISTER_SUCCESS_EMAIL);
        assertThat(dbCount).isEqualTo(1);

        // Verify newly registered user can login immediately
        ResponseEntity<String> loginResponse = login(REGISTER_SUCCESS_EMAIL, "ValidPass123!");
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.<String>read(loginResponse.getBody(), "$.token")).isNotBlank();
    }

    private ResponseEntity<String> register(String email, String password, String confirm, boolean agree) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange("/api/v1/auth/register", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "fullName", "Story 1.3 Register Test",
                        "phone", "0909999999",
                        "email", email,
                        "password", password,
                        "confirmPassword", confirm,
                        "agreeToTerms", agree), headers), String.class);
    }

    private ResponseEntity<String> login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange("/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("email", email, "password", password), headers), String.class);
    }

    private long countAuditRows(String where) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM activity_logs WHERE " + where, Long.class);
        return count == null ? 0 : count;
    }
}
