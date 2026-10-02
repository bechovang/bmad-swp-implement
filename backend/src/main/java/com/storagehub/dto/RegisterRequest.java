package com.storagehub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/auth/register body (contract: RegisterRequest). No role
 * field exists and a stray one in the JSON is ignored during binding -
 * register is customer-only (AD-5). Cross-field rules (confirmPassword
 * match, agreeToTerms) are settled in the service with fieldErrors, not by
 * class-level constraints (defer F18).
 */
public record RegisterRequest(

        @NotBlank(message = "fullName is required")
        @Size(max = 100, message = "fullName must be at most 100 characters")
        String fullName,

        @NotBlank(message = "phone is required")
        @Size(max = 20, message = "phone must be at most 20 characters")
        String phone,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 100, message = "email must be at most 100 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(max = 72, message = "password must be at most 72 characters")
        String password,

        @NotBlank(message = "confirmPassword is required")
        @Size(max = 72, message = "confirmPassword must be at most 72 characters")
        String confirmPassword,

        @NotNull(message = "agreeToTerms is required")
        Boolean agreeToTerms) {
}
