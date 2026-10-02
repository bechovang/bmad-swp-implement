package com.storagehub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test: the Spring context loads against a local MySQL with the dev profile
 * (V1 schema + V2 demo seed). Requires the env vars from README ("Backend env vars")
 * and a running MySQL - it catches wiring/config breakage early (story 1.1).
 * Skips (does not fail) when DB_URL is unset, e.g. on a laptop without MySQL.
 * Story 1.2 also locks the security chain end-to-end over real HTTP: the one
 * public path stays 200 and everything else answers the 401 envelope.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class StorageHubApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

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
}
