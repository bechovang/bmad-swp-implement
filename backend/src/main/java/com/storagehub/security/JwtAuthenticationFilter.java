package com.storagehub.security;

import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.repository.UserRepository;
import com.storagehub.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Per-request JWT authentication (AD-5). Parses the Bearer token, verifies
 * signature/expiry via {@link JwtService}, then re-checks the account is
 * still ACTIVE against users.Status through a short-lived in-memory cache -
 * so a user locked or deactivated after the token was issued loses access
 * within ~30s, not at token TTL. Single-node assumption by design (epic
 * context). Every failure (missing/garbage/expired token, deleted, inactive
 * or locked user) leaves through the envelope entry point as the same 401 -
 * the default Spring body never reaches the client. Requests without a
 * Bearer header pass through untouched and stay anonymous (for the chain to
 * answer 401 on guarded paths).
 *
 * Not a @Component on purpose: it is declared as a @Bean in SecurityConfig
 * with explicit dependencies, so test slices that import the chain get
 * exactly its real wiring.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Staleness window of the users.Status re-check (spec: ~30s). */
    static final long STATUS_CACHE_TTL_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final ConcurrentHashMap<Long, CachedStatus> statusCache = new ConcurrentHashMap<>();

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository,
            AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("/actuator/health".equals(path)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(request.getMethod()) && (
                "/api/v1/auth/login".equals(path) ||
                "/api/v1/auth/register".equals(path) ||
                "/api/v1/auth/forgot-password".equals(path))) {
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        UsernamePasswordAuthenticationToken authentication = null;
        try {
            JwtService.VerifiedToken verified = jwtService.parse(header.substring("Bearer ".length()));
            if (!statusIsEligible(verified.userId())) {
                reject(request, response,
                        new AuthenticationServiceException("Account is not active"));
                return;
            }
            authentication = UsernamePasswordAuthenticationToken.authenticated(verified.userId().toString(), verified,
                    List.of(new SimpleGrantedAuthority(verified.role().authority())));
        } catch (JwtException | IllegalArgumentException ex) {
            reject(request, response, new BadCredentialsException("Invalid or expired token", ex));
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    /**
     * ACTIVE check with the ~30s cache: a fresh cached status short-circuits
     * the DB read; an expired entry refetches. A user that no longer exists
     * counts as not eligible and drops its cache entry.
     */
    private boolean statusIsEligible(Long userId) {
        long now = System.currentTimeMillis();
        CachedStatus cached = statusCache.get(userId);
        if (cached != null && now - cached.fetchedAtMillis() < STATUS_CACHE_TTL_MILLIS) {
            return cached.status() == UserStatus.ACTIVE;
        }
        UserStatus fresh = userRepository.findById(userId).map(User::getStatus).orElse(null);
        if (fresh == null) {
            statusCache.remove(userId);
            return false;
        }
        statusCache.put(userId, new CachedStatus(fresh, now));
        return fresh == UserStatus.ACTIVE;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response, exception);
    }

    /** Test hook: simulates the TTL passing, without waiting 30 wall-clock seconds. */
    void clearStatusCache() {
        statusCache.clear();
    }

    private record CachedStatus(UserStatus status, long fetchedAtMillis) {
    }
}
