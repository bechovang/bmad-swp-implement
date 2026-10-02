package com.storagehub.security;

import com.storagehub.entity.Role;
import com.storagehub.entity.RoleName;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.repository.UserRepository;
import com.storagehub.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests of the per-request filter (no Spring context, no DB): Bearer
 * parsing sets ROLE_<ROLE> authorities, every failure leaves through the
 * entry point as the same 401, requests without a Bearer header pass through
 * untouched (anonymous), and the users.Status re-check honors the ~30s
 * cache - a user locked after issuance is cut off as soon as the cache
 * entry expires (clearStatusCache stands in for waiting the TTL).
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTests {

    private static final JwtService JWT_SERVICE =
            new JwtService(new JwtProperties("filter-test-secret-0123456789-0123456789", 24));

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.web.AuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(JWT_SERVICE, userRepository, authenticationEntryPoint);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenForAnActiveUserAuthenticatesWithTheMatrixAuthority() throws Exception {
        stubUser(7L, UserStatus.ACTIVE);
        String token = JWT_SERVICE.issueToken(7L, RoleName.BUSINESS_OPS);

        MockHttpServletRequest request = requestWithToken(token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).singleElement()
                .satisfies(authority -> assertThat(authority.getAuthority()).isEqualTo("ROLE_BUSINESS_OPS"));
        assertThat(authentication.getName()).isEqualTo("7");
    }

    @Test
    void userLockedAfterTokenIssuanceIsRejectedOnceTheStatusCacheExpires() throws Exception {
        stubUser(8L, UserStatus.ACTIVE);
        String token = JWT_SERVICE.issueToken(8L, RoleName.CUSTOMER);

        filter.doFilter(requestWithToken(token), new MockHttpServletResponse(), filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();

        // Same user, now locked in the DB - inside the TTL the cached status still admits
        // (a FRESH request each time: OncePerRequestFilter skips repeated filter attribute).
        stubUser(8L, UserStatus.LOCKED);
        filter.doFilter(requestWithToken(token), new MockHttpServletResponse(), filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();

        // TTL later (simulated): the refetch sees LOCKED and the request is cut off.
        filter.clearStatusCache();
        filter.doFilter(requestWithToken(token), new MockHttpServletResponse(), filterChain);
        verify(authenticationEntryPoint).commence(any(), any(), any(AuthenticationException.class));
    }

    @Test
    void deletedUserIsRejected() throws Exception {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        String token = JWT_SERVICE.issueToken(9L, RoleName.CUSTOMER);

        filter.doFilter(requestWithToken(token), new MockHttpServletResponse(), filterChain);

        verify(authenticationEntryPoint).commence(any(), any(), any(AuthenticationException.class));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void expiredTokenIsRejectedThroughTheEntryPoint() throws Exception {
        String token = JWT_SERVICE.issueToken(10L, RoleName.CUSTOMER, java.time.Duration.ofSeconds(-60));

        filter.doFilter(requestWithToken(token), new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(any(), any(), any(AuthenticationException.class));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void garbageTokenIsRejectedThroughTheEntryPoint() throws Exception {
        filter.doFilter(requestWithToken("not-a-jwt"), new MockHttpServletResponse(), filterChain);

        verify(authenticationEntryPoint).commence(any(), any(), any(AuthenticationException.class));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void requestsWithoutABearerHeaderPassThroughUntouched() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anything");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void nonBearerAuthorizationHeaderPassesThroughToo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anything");
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(filterChain).doFilter(any(), any());
        verify(authenticationEntryPoint, never()).commence(any(), any(), any());
    }

    private void stubUser(long id, UserStatus status) {
        User user = new User("Test User", "user@storagehub.dev", "0900000000",
                "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu",
                new Role(1, RoleName.CUSTOMER.titleCase(), null), status);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
    }

    private static MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/__test/customer-only");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
