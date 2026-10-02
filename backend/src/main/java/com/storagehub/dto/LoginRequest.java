package com.storagehub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/auth/login body (contract: LoginRequest). Field-level
 * validation is intentionally minimal: which side was wrong is never
 * revealed, all credential failures share one generic 401 message.
 */
public record LoginRequest(

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 100, message = "email must be at most 100 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(max = 72, message = "password must be at most 72 characters")
        String password) {
}
