package com.storagehub.controller;

import com.storagehub.dto.ForgotPasswordRequest;
import com.storagehub.dto.ForgotPasswordResponse;
import com.storagehub.dto.LoginRequest;
import com.storagehub.dto.LoginResponse;
import com.storagehub.dto.RegisterRequest;
import com.storagehub.dto.RegisterResponse;
import com.storagehub.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The three public auth endpoints (AD-5, contract: /auth/*). HTTP <-> DTO
 * only; every decision lives in AuthService (AD-3). All three are permitAll
 * in SecurityConfig - authentication happens here by credentials, not by
 * token.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 200 {token, user} - or 401 with the one shared failure message. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** 201 - always a CUSTOMER, active account; never a token. */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    /** 200 - the same generic answer for every email. */
    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }
}
