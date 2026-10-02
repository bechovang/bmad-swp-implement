package com.storagehub.service;

import com.storagehub.dto.AuthUser;
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
import com.storagehub.dto.FieldError;
import com.storagehub.repository.RoleRepository;
import com.storagehub.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The three auth use cases of story 1.3 (AD-5). Login ALWAYS walks the same
 * lookup + BCrypt path on every branch - including a hash comparison when
 * the email matches no user - so timing and the response cannot reveal
 * which side failed or whether the email exists; every failure throws
 * {@link InvalidCredentialsException} with the one shared message AFTER the
 * LOGIN_FAILED row is appended (unknown email: ActorID NULL, Reason carries
 * the attempted email - Q1=A). Register is customer-only by construction -
 * the API has no role field - and is not audited (no REGISTER action in the
 * 1.2 registry). Forgot-password answers generically without touching the
 * database at all.
 */
@Service
public class AuthService {

    /**
     * Valid cost-10 $2a$ hash (the V2 seed demo hash) used as the bcrypt
     * operand on the unknown-email branch so that branch spends the same
     * ~cost as the real one. The comparison result is discarded there.
     */
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$kB0vQAnKi.enlB8C.EgxR.jSPrdJiQcN9ODdOz.OCRFNIFDB/.aqu";

    /** EntityID stored with a LOGIN_FAILED row when no account matches the email. */
    private static final long NO_ENTITY = 0L;

    private static final String LOGIN_FAILED_REASON = "Sign-in failed";
    private static final String LOGIN_FAILED_UNKNOWN_EMAIL_REASON_PREFIX = "Sign-in failed - unknown email: ";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LogService logService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService, LogService logService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.logService = logService;
    }

    /**
     * Deliberately NOT @Transactional: the LOGIN/LOGIN_FAILED row must
     * commit even though the failure branches throw - LogService.append
     * runs synchronously in its own committed transaction (AD-6: never
     * REQUIRES_NEW inside LogService, so the caller stays out of one).
     *
     * @throws InvalidCredentialsException unknown email, wrong password, or
     *         Status 0/2 - always the same shared message (NFR-7)
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailWithRole(request.email()).orElse(null);
        String hashUnderTest = user != null ? user.getPasswordHash() : DUMMY_BCRYPT_HASH;
        boolean passwordMatches = false;
        try {
            passwordMatches = passwordEncoder.matches(request.password(), hashUnderTest);
        } catch (IllegalArgumentException ex) {
            passwordMatches = false;
        }

        if (user == null || !passwordMatches || user.getStatus() != UserStatus.ACTIVE) {
            String emailAttempt = request.email() != null ? request.email() : "";
            int maxEmailLen = LogService.MAX_TEXT_LENGTH - LOGIN_FAILED_UNKNOWN_EMAIL_REASON_PREFIX.length();
            String safeEmail = emailAttempt.length() > maxEmailLen ? emailAttempt.substring(0, maxEmailLen) : emailAttempt;
            logService.append(
                    user != null ? user.getId() : null,
                    EntityType.USER,
                    user != null ? user.getId() : NO_ENTITY,
                    Action.LOGIN_FAILED,
                    null,
                    null,
                    user != null ? LOGIN_FAILED_REASON
                            : LOGIN_FAILED_UNKNOWN_EMAIL_REASON_PREFIX + safeEmail);
            throw new InvalidCredentialsException();
        }

        logService.append(user.getId(), EntityType.USER, user.getId(), Action.LOGIN, null, null, null);
        return new LoginResponse(jwtService.issueToken(user.getId(), user.roleName()), AuthUser.from(user));
    }

    /**
     * @throws InvalidRequestException confirmPassword mismatch, terms not
     *         accepted, or duplicate email - one fieldError per cause (400)
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        List<FieldError> fieldErrors = new ArrayList<>();
        if (!Objects.equals(request.password(), request.confirmPassword())) {
            fieldErrors.add(new FieldError("confirmPassword",
                    "Password confirmation does not match the password."));
        }
        if (!Boolean.TRUE.equals(request.agreeToTerms())) {
            fieldErrors.add(new FieldError("agreeToTerms",
                    "You must accept the terms to create an account."));
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            fieldErrors.add(new FieldError("email",
                    "An account with this email already exists. Sign in instead or use another email."));
        }
        if (!fieldErrors.isEmpty()) {
            throw new InvalidRequestException(fieldErrors);
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER.titleCase())
                .orElseThrow(() -> new IllegalStateException(
                        "Role 'Customer' is missing - users table needs the V2 demo seed role"));
        try {
            User saved = userRepository.save(new User(
                    request.fullName(),
                    request.email(),
                    request.phone(),
                    passwordEncoder.encode(request.password()),
                    customerRole,
                    UserStatus.ACTIVE));
            return RegisterResponse.from(AuthUser.from(saved));
        } catch (DataIntegrityViolationException ex) {
            throw new InvalidRequestException(List.of(new FieldError("email",
                    "An account with this email already exists. Sign in instead or use another email.")));
        }
    }

    /** Generic answer for every email - no lookup, no leak (P2 stub). */
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        return ForgotPasswordResponse.generic();
    }
}
