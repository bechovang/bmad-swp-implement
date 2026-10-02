package com.storagehub.service;

import com.storagehub.dto.ForgotPasswordRequest;
import com.storagehub.dto.ForgotPasswordResponse;
import com.storagehub.dto.LoginRequest;
import com.storagehub.dto.LoginResponse;
import com.storagehub.dto.RegisterRequest;
import com.storagehub.dto.RegisterResponse;
import com.storagehub.entity.Action;
import com.storagehub.entity.EntityType;
import com.storagehub.entity.Role;
import com.storagehub.entity.RoleName;
import com.storagehub.entity.User;
import com.storagehub.entity.UserStatus;
import com.storagehub.exception.InvalidCredentialsException;
import com.storagehub.exception.InvalidRequestException;
import com.storagehub.repository.RoleRepository;
import com.storagehub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The story 1.3 login/register/forgot matrix with mocked persistence: the
 * shared failure message, the LOGIN/LOGIN_FAILED audit rows (including the
 * NULL-actor unknown-email row of Q1=A), register's customer-only creation
 * with a real BCrypt encoder (strength 10, like the V2 seed), cross-field
 * and duplicate-email fieldErrors, and the generic forgot answer. All
 * failure branches must produce the identical message.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    /** V2 seed hash of Demo1234! - a real cost-10 $2a$ hash. */
    private static final String DEMO_HASH =
            "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu";

    private static final String DEMO_PASSWORD = "Demo1234!";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private LogService logService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService,
                logService);
    }

    // ------------------------------------------------------------- login

    @Test
    void loginSuccessReturnsTokenUpperSnakeRoleAndAuditsLogin() {
        User lan = user(1L, RoleName.CUSTOMER, UserStatus.ACTIVE);
        when(userRepository.findByEmailWithRole("lan@storagehub.dev")).thenReturn(Optional.of(lan));
        when(jwtService.issueToken(1L, RoleName.CUSTOMER)).thenReturn("jwt-value");

        LoginResponse response = authService.login(new LoginRequest("lan@storagehub.dev", DEMO_PASSWORD));

        assertThat(response.token()).isEqualTo("jwt-value");
        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().fullName()).isEqualTo("Lan Nguyen");
        assertThat(response.user().role()).isEqualTo("CUSTOMER");
        verify(logService).append(1L, EntityType.USER, 1L, Action.LOGIN, null, null, null);
        verify(logService, never()).append(any(), any(), any(),
                org.mockito.ArgumentMatchers.eq(Action.LOGIN_FAILED), any(), any(), any());
    }

    @Test
    void wrongPasswordThrowsTheSharedMessageAndAuditsFailedLoginForTheUser() {
        User lan = user(1L, RoleName.CUSTOMER, UserStatus.ACTIVE);
        when(userRepository.findByEmailWithRole("lan@storagehub.dev")).thenReturn(Optional.of(lan));

        InvalidCredentialsException thrown = catchThrowableOfType(
                () -> authService.login(new LoginRequest("lan@storagehub.dev", "WrongPass1")),
                InvalidCredentialsException.class);

        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).isEqualTo(InvalidCredentialsException.SHARED_MESSAGE);
        verify(logService).append(1L, EntityType.USER, 1L, Action.LOGIN_FAILED, null, null,
                "Sign-in failed");
    }

    @Test
    void unknownEmailThrowsTheSameMessageAndAuditsFailedLoginWithNullActor() {
        when(userRepository.findByEmailWithRole("ghost@storagehub.dev")).thenReturn(Optional.empty());

        InvalidCredentialsException thrown = catchThrowableOfType(
                () -> authService.login(new LoginRequest("ghost@storagehub.dev", "Whatever123")),
                InvalidCredentialsException.class);

        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).isEqualTo(InvalidCredentialsException.SHARED_MESSAGE);
        verify(logService).append(null, EntityType.USER, 0L, Action.LOGIN_FAILED, null, null,
                "Sign-in failed - unknown email: ghost@storagehub.dev");
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = { "INACTIVE", "LOCKED" })
    void nonActiveStatusIsRejectedLikeAWrongPasswordEvenWithTheRightPassword(UserStatus status) {
        User user = user(5L, RoleName.SYSTEM_ADMINISTRATOR, status);
        when(userRepository.findByEmailWithRole("nam@storagehub.dev")).thenReturn(Optional.of(user));

        InvalidCredentialsException thrown = catchThrowableOfType(
                () -> authService.login(new LoginRequest("nam@storagehub.dev", DEMO_PASSWORD)),
                InvalidCredentialsException.class);

        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage()).isEqualTo(InvalidCredentialsException.SHARED_MESSAGE);
        verify(logService).append(5L, EntityType.USER, 5L, Action.LOGIN_FAILED, null, null,
                "Sign-in failed");
    }

    @Test
    void managerRoleTravelsUpperSnakeOnTheWireAndTheToken() {
        User tuan = user(3L, RoleName.FACILITY_MANAGER, UserStatus.ACTIVE);
        when(userRepository.findByEmailWithRole("tuan@storagehub.dev")).thenReturn(Optional.of(tuan));
        when(jwtService.issueToken(3L, RoleName.FACILITY_MANAGER)).thenReturn("jwt-tuan");

        LoginResponse response = authService.login(new LoginRequest("tuan@storagehub.dev", DEMO_PASSWORD));

        assertThat(response.user().role()).isEqualTo("FACILITY_MANAGER");
        assertThat(response.token()).isEqualTo("jwt-tuan");
    }

    // ---------------------------------------------------------- register

    @Test
    void registerCreatesTheOnlyAllowedRoleActiveCustomerWithABcryptHash() {
        Role customer = new Role(1, "Customer", null);
        when(userRepository.findByEmail("new@storagehub.dev")).thenReturn(Optional.empty());
        when(roleRepository.findByName("Customer")).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = authService.register(validRegister());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getFullName()).isEqualTo("New User");
        assertThat(saved.getEmail()).isEqualTo("new@storagehub.dev");
        assertThat(saved.getPhone()).isEqualTo("0909999999");
        assertThat(saved.getRole().getName()).isEqualTo("Customer");
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(passwordEncoder.matches("MyPass123!", saved.getPasswordHash()))
                .as("stored hash must be a BCrypt hash of the raw password (strength 10)")
                .isTrue();
        assertThat(saved.getPasswordHash()).startsWith("$2a$10$");
        assertThat(response.role()).isEqualTo("CUSTOMER");
        assertThat(response.email()).isEqualTo("new@storagehub.dev");
        verifyNoInteractions(logService); // register is not audited (no REGISTER action in 1.2)
    }

    @Test
    void registerDuplicateEmailYieldsTheEmailFieldError() {
        when(userRepository.findByEmail("lan@storagehub.dev"))
                .thenReturn(Optional.of(user(1L, RoleName.CUSTOMER, UserStatus.ACTIVE)));

        InvalidRequestException thrown = catchThrowableOfType(() -> authService.register(
                new RegisterRequest("New User", "0909999999", "lan@storagehub.dev",
                        "MyPass123!", "MyPass123!", true)),
                InvalidRequestException.class);

        assertThat(thrown).isNotNull();
        assertThat(thrown.fieldErrors()).extracting("field").containsExactly("email");
    }

    @Test
    void registerPasswordConfirmationMismatchYieldsTheConfirmFieldError() {
        when(userRepository.findByEmail("new@storagehub.dev")).thenReturn(Optional.empty());

        InvalidRequestException thrown = catchThrowableOfType(() -> authService.register(
                new RegisterRequest("New User", "0909999999", "new@storagehub.dev",
                        "MyPass123!", "MyPass456!", true)),
                InvalidRequestException.class);

        assertThat(thrown).isNotNull();
        assertThat(thrown.fieldErrors()).extracting("field").containsExactly("confirmPassword");
    }

    @Test
    void registerTermsNotAcceptedYieldsTheAgreeFieldError() {
        when(userRepository.findByEmail("new@storagehub.dev")).thenReturn(Optional.empty());

        InvalidRequestException thrown = catchThrowableOfType(() -> authService.register(
                new RegisterRequest("New User", "0909999999", "new@storagehub.dev",
                        "MyPass123!", "MyPass123!", false)),
                InvalidRequestException.class);

        assertThat(thrown).isNotNull();
        assertThat(thrown.fieldErrors()).extracting("field").containsExactly("agreeToTerms");
    }

    @Test
    void registerConcurrentEmailCollisionCaughtAsInvalidRequestException() {
        Role customer = new Role(1, "Customer", null);
        when(userRepository.findByEmail("new@storagehub.dev")).thenReturn(Optional.empty());
        when(roleRepository.findByName("Customer")).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate entry"));

        InvalidRequestException thrown = catchThrowableOfType(
                () -> authService.register(validRegister()),
                InvalidRequestException.class);

        assertThat(thrown).isNotNull();
        assertThat(thrown.fieldErrors()).extracting("field").containsExactly("email");
    }

    // ------------------------------------------------------------ forgot

    @Test
    void forgotPasswordAlwaysAnswersTheSameGenericMessageWithoutTouchingTheDatabase() {
        ForgotPasswordResponse response =
                authService.forgotPassword(new ForgotPasswordRequest("anyone@storagehub.dev"));

        assertThat(response.message()).isEqualTo(ForgotPasswordResponse.GENERIC_MESSAGE);
        verifyNoInteractions(userRepository, logService);
    }

    // ----------------------------------------------------------- helpers

    private static RegisterRequest validRegister() {
        return new RegisterRequest("New User", "0909999999", "new@storagehub.dev",
                "MyPass123!", "MyPass123!", true);
    }

    private static User user(long id, RoleName role, UserStatus status) {
        User user = new User("Lan Nguyen", emailOf(role), "0901234567", DEMO_HASH,
                new Role((int) id, role.titleCase(), null), status);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static String emailOf(RoleName role) {
        return switch (role) {
            case CUSTOMER -> "lan@storagehub.dev";
            case STAFF -> "minh@storagehub.dev";
            case FACILITY_MANAGER -> "tuan@storagehub.dev";
            case BUSINESS_OPS -> "hang@storagehub.dev";
            case SYSTEM_ADMINISTRATOR -> "nam@storagehub.dev";
        };
    }
}
