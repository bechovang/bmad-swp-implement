package com.storagehub.service;

import com.storagehub.entity.RoleName;
import com.storagehub.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests of the token itself (no Spring context): HS256 round-trip,
 * the frozen claim set (sub/role/iat/exp only), the configured 24h TTL,
 * expiry verification, signature forgery and out-of-matrix role claims.
 */
class JwtServiceTests {

    private static final String SECRET = "unit-test-secret-0123456789-0123456789";

    private static final JwtProperties PROPERTIES = new JwtProperties(SECRET, 24);

    private final JwtService jwtService = new JwtService(PROPERTIES);

    @Test
    void issuedTokenVerifiesAndCarriesSubjectAndUpperSnakeRole() {
        String token = jwtService.issueToken(42L, RoleName.FACILITY_MANAGER);

        JwtService.VerifiedToken verified = jwtService.parse(token);

        assertThat(verified.userId()).isEqualTo(42L);
        assertThat(verified.role()).isEqualTo(RoleName.FACILITY_MANAGER);
    }

    @Test
    void claimsAreExactlySubRoleIatExp() {
        String token = jwtService.issueToken(7L, RoleName.CUSTOMER);

        Claims claims = parseWithTheSameKey(token);
        assertThat(claims.keySet()).containsExactlyInAnyOrder(Claims.SUBJECT, "role",
                Claims.ISSUED_AT, Claims.EXPIRATION);
        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
    }

    @Test
    void expirationIsIssuedAtPlusTheConfiguredTtl() {
        String token = jwtService.issueToken(7L, RoleName.CUSTOMER);

        Claims claims = parseWithTheSameKey(token);
        long ttlSeconds = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;
        assertThat(ttlSeconds).isEqualTo(Duration.ofHours(24).toSeconds());
    }

    @Test
    void expiredTokenIsRejected() {
        String token = jwtService.issueToken(7L, RoleName.CUSTOMER, Duration.ofSeconds(-60));

        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {
        JwtService forger = new JwtService(new JwtProperties("x".repeat(48), 24));
        String forged = forger.issueToken(7L, RoleName.CUSTOMER);

        assertThatThrownBy(() -> jwtService.parse(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void roleOutsideThePermissionMatrixIsRejected() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String token = Jwts.builder()
                .subject("7")
                .claim("role", "SUPERUSER")
                .issuedAt(new java.util.Date())
                .expiration(java.util.Date.from(java.time.Instant.now().plus(Duration.ofHours(1))))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("SUPERUSER");
    }

    @Test
    void nonNumericSubjectIsRejected() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String token = Jwts.builder()
                .subject("not-a-user-id")
                .claim("role", "CUSTOMER")
                .issuedAt(new java.util.Date())
                .expiration(java.util.Date.from(java.time.Instant.now().plus(Duration.ofHours(1))))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void missingRoleClaimIsRejected() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("7")
                .issuedAt(new java.util.Date())
                .expiration(java.util.Date.from(java.time.Instant.now().plus(Duration.ofHours(1))))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("role");
    }

    @Test
    void missingExpirationClaimIsRejected() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("7")
                .claim("role", "CUSTOMER")
                .issuedAt(new java.util.Date())
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("expiration");
    }

    private Claims parseWithTheSameKey(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
