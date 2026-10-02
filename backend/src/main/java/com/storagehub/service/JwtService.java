package com.storagehub.service;

import com.storagehub.entity.RoleName;
import com.storagehub.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Signs and verifies the auth tokens (AD-5): JWT HS256, claims exactly
 * sub = UserID, role = RoleName (UPPER_SNAKE), iat, exp = iat + app.jwt.
 * ttl-hours (24). No refresh token, no other claims - nothing sensitive
 * rides in the token. Tokens are stateless; the per-request status
 * re-check lives in the security filter, not here.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration ttl;

    public JwtService(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofHours(properties.ttlHours());
    }

    /** Issues a token for a signed-in user with the configured TTL. */
    public String issueToken(Long userId, RoleName role) {
        return issueToken(userId, role, ttl);
    }

    /**
     * Issues a token with an explicit lifetime. Public for tests that need
     * already-expired or long-lived tokens; production callers use
     * {@link #issueToken(Long, RoleName)}.
     */
    public String issueToken(Long userId, RoleName role, Duration lifetime) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(lifetime)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verified parse: signature, expiry and the fixed claim shape.
     *
     * @throws JwtException signature forged, token expired, or claims not in
     *         the sub/role shape this service issues (sub not numeric /
     *         unknown role) - the filter turns every variant into the same
     *         401 envelope
     */
    public VerifiedToken parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (claims.getExpiration() == null) {
            throw new JwtException("Token has no expiration claim");
        }
        return new VerifiedToken(parseSubject(claims), parseRole(claims));
    }

    private static Long parseSubject(Claims claims) {
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new JwtException("Token subject is not a UserID: " + claims.getSubject(), ex);
        }
    }

    private static RoleName parseRole(Claims claims) {
        Object role = claims.get("role");
        if (role == null) {
            throw new JwtException("Token has no role claim");
        }
        try {
            return RoleName.valueOf(role.toString());
        } catch (IllegalArgumentException ex) {
            throw new JwtException("Token role is outside the permission matrix: " + role, ex);
        }
    }

    /** A verified token's identity half: who (UserID) and as what (matrix role). */
    public record VerifiedToken(Long userId, RoleName role) {
    }
}
