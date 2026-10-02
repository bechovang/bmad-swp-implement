package com.storagehub.controller;

import com.storagehub.config.SecurityConfig;
import com.storagehub.dto.AuthUser;
import com.storagehub.dto.ForgotPasswordRequest;
import com.storagehub.dto.ForgotPasswordResponse;
import com.storagehub.dto.LoginRequest;
import com.storagehub.dto.LoginResponse;
import com.storagehub.dto.RegisterRequest;
import com.storagehub.dto.RegisterResponse;
import com.storagehub.entity.Role;
import com.storagehub.entity.RoleName;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.repository.UserRepository;
import com.storagehub.security.EnvelopeAccessDeniedHandler;
import com.storagehub.security.EnvelopeAuthenticationEntryPoint;
import com.storagehub.service.AuthService;
import com.storagehub.service.JwtService;
import com.storagehub.dto.FieldError;
import com.storagehub.exception.InvalidCredentialsException;
import com.storagehub.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Story 1.3 permission matrix driven through the REAL chain (SecurityConfig
 * + JwtAuthenticationFilter + JwtService; only the repository and AuthService
 * are mocks) with tokens signed by the real test JwtService: the three auth
 * paths stay public without a token, a valid token authenticates, the wrong
 * role trips method security into the FORBIDDEN envelope (the latent 1.2
 * fix), and a locked user is cut off by the filter's status re-check. Each
 * test uses its own UserID because the filter's status cache lives for the
 * whole context (~30s TTL).
 */
@WebMvcTest(controllers = { AuthController.class, TestPingController.class }, properties =
        "app.jwt.secret=chain-test-secret-0123456789-abcdefghij")
@Import({ SecurityConfig.class, JwtService.class, EnvelopeAuthenticationEntryPoint.class,
        EnvelopeAccessDeniedHandler.class, ErrorEnvelopeWriter.class })
class AuthSecurityChainTests {

    private static final String CUSTOMER_TOKEN_USER = "101";
    private static final String STAFF_TOKEN_USER = "102";
    private static final String LOCKED_TOKEN_USER = "103";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    // ------------------------------------------------------ public paths

    @Test
    void theThreeAuthPathsStayPublicWithoutAnyToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(
                new LoginResponse("a-token", new AuthUser(1L, "Lan Nguyen", "lan@storagehub.dev",
                        "CUSTOMER", "0901234567")));
        when(authService.register(any(RegisterRequest.class))).thenReturn(RegisterResponse.from(
                new AuthUser(9L, "New User", "new@storagehub.dev", "CUSTOMER", "0909999999")));
        when(authService.forgotPassword(any(ForgotPasswordRequest.class)))
                .thenReturn(ForgotPasswordResponse.generic());

        postJson("/api/v1/auth/login", "{\"email\":\"lan@storagehub.dev\",\"password\":\"Demo1234!\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("a-token"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));

        postJson("/api/v1/auth/register", validRegisterJson())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        postJson("/api/v1/auth/forgot-password", "{\"email\":\"anyone@storagehub.dev\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(ForgotPasswordResponse.GENERIC_MESSAGE));
    }

    @Test
    void registerBindsTheBodyAndIgnoresAStrayRoleField() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(RegisterResponse.from(
                new AuthUser(9L, "New User", "new@storagehub.dev", "CUSTOMER", "0909999999")));

        String json = validRegisterJson().replaceFirst("\\{", "\\{\"role\":\"SYSTEM_ADMINISTRATOR\",");
        postJson("/api/v1/auth/register", json).andExpect(status().isCreated());

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new RegisterRequest(
                "New User", "0909999999", "new@storagehub.dev", "MyPass123!", "MyPass123!", true));
    }

    // --------------------------------------------------- token requests

    @Test
    void validTokenForAnActiveUserPassesTheMatrixForItsRole() throws Exception {
        stubUser(CUSTOMER_TOKEN_USER, RoleName.CUSTOMER, UserStatus.ACTIVE);
        String token = jwtService.issueToken(101L, RoleName.CUSTOMER);

        mockMvc.perform(get("/api/v1/__test/customer-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void validTokenWithTheWrongRoleGetsThe403EnvelopeThroughMethodSecurity() throws Exception {
        stubUser(STAFF_TOKEN_USER, RoleName.STAFF, UserStatus.ACTIVE);
        String token = jwtService.issueToken(102L, RoleName.STAFF);

        mockMvc.perform(get("/api/v1/__test/customer-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void stillValidTokenOfAUserLockedAfterIssuanceIsCutOffByTheFilter() throws Exception {
        stubUser(LOCKED_TOKEN_USER, RoleName.CUSTOMER, UserStatus.LOCKED);
        String token = jwtService.issueToken(103L, RoleName.CUSTOMER);

        mockMvc.perform(get("/api/v1/__test/customer-only").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void garbageTokenAnswersThe401EnvelopeWithTheChallengeHeader() throws Exception {
        mockMvc.perform(get("/api/v1/__test/ping").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void loginWithInvalidCredentialsAnswers401EnvelopeWithChallengeHeader() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        postJson("/api/v1/auth/login", "{\"email\":\"lan@storagehub.dev\",\"password\":\"WrongPassword!\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value(InvalidCredentialsException.SHARED_MESSAGE));
    }

    @Test
    void registerWithInvalidRequestAnswers400EnvelopeWithFieldErrors() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new InvalidRequestException(List.of(
                        new FieldError("email", "An account with this email already exists."))));

        postJson("/api/v1/auth/register", validRegisterJson())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    void publicAuthEndpointWithInvalidBearerTokenBypassesFilterAndReachesController() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(
                new LoginResponse("a-token", new AuthUser(1L, "Lan Nguyen", "lan@storagehub.dev",
                        "CUSTOMER", "0901234567")));

        mockMvc.perform(post("/api/v1/auth/login")
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"lan@storagehub.dev\",\"password\":\"Demo1234!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("a-token"));
    }

    // ------------------------------------------------------ test helpers

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static String validRegisterJson() {
        return "{\"fullName\":\"New User\",\"phone\":\"0909999999\",\"email\":\"new@storagehub.dev\","
                + "\"password\":\"MyPass123!\",\"confirmPassword\":\"MyPass123!\",\"agreeToTerms\":true}";
    }

    private void stubUser(String userId, RoleName role, UserStatus status) {
        User user = new User("Test User", userId + "@storagehub.dev", "0900000000",
                "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu",
                new Role(role.ordinal() + 1, role.titleCase(), null), status);
        when(userRepository.findById(Long.valueOf(userId))).thenReturn(Optional.of(user));
    }
}
