package com.storagehub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/auth/forgot-password body (contract: ForgotPasswordRequest).
 * The answer never depends on the email - see ForgotPasswordResponse.
 */
public record ForgotPasswordRequest(

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 100, message = "email must be at most 100 characters")
        String email) {
}
