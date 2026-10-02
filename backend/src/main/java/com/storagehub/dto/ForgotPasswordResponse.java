package com.storagehub.dto;

/**
 * POST /api/v1/auth/forgot-password 200 body (contract:
 * ForgotPasswordResponse). One fixed generic sentence for every request -
 * account existence is never revealed (P2: no real email in v1).
 */
public record ForgotPasswordResponse(String message) {

    /** The one string every caller sees, whether or not the account exists. */
    public static final String GENERIC_MESSAGE =
            "If an account exists for this email, a password reset message has been sent to it. "
                    + "Check the inbox and follow the instructions to set a new password.";

    public static ForgotPasswordResponse generic() {
        return new ForgotPasswordResponse(GENERIC_MESSAGE);
    }
}
