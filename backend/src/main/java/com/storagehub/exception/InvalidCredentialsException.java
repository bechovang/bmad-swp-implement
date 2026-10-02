package com.storagehub.exception;

/**
 * Login failure (wrong email, wrong password, inactive or locked account).
 * Carries the ONE shared message for every branch - never which field
 * failed, never whether the email exists (AD-5/NFR-7). GlobalExceptionHandler
 * renders it as the 401 UNAUTHENTICATED envelope; the audit row
 * (LOGIN_FAILED) is written before this is thrown.
 */
public class InvalidCredentialsException extends RuntimeException {

    /** The single generic message for every login failure branch. */
    public static final String SHARED_MESSAGE =
            "The email or password is not correct. No data was returned. "
                    + "Check your credentials and try again, or reset the password via forgot password.";

    public InvalidCredentialsException() {
        super(SHARED_MESSAGE);
    }
}
