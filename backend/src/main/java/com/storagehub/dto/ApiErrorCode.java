package com.storagehub.dto;

import org.springframework.http.HttpStatus;

/**
 * Framework error codes of the error envelope (AD-8) - the fixed catalog every
 * non-2xx response uses unless a business rule supplies its own code (409, see
 * com.storagehub.exception.BusinessRuleException). The same catalog is listed
 * in the Error.code description of contracts/openapi.yaml; the two must stay
 * in sync. Messages follow NFR-7: what happened + consequence + exactly one
 * next step, no exclamation marks.
 */
public enum ApiErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST,
            "Some fields are missing or invalid. Nothing was saved. "
                    + "Correct the highlighted fields and submit again."),

    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST,
            "The request could not be read. Nothing was changed. "
                    + "Check the request format and retry."),

    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED,
            "A valid access token is required to access this resource. "
                    + "No data was returned. Sign in and retry the request."),

    FORBIDDEN(HttpStatus.FORBIDDEN,
            "Your account does not have permission for this action. "
                    + "Nothing was changed. Contact an administrator if you need access."),

    NOT_FOUND(HttpStatus.NOT_FOUND,
            "The requested resource does not exist. No data was returned. "
                    + "Check the address or return to the list."),

    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED,
            "The HTTP method is not supported on this endpoint. Nothing was changed. "
                    + "Use the method documented for this resource."),

    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "The request media type is not supported. Nothing was changed. "
                    + "Send the request as application/json."),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Nothing was changed. "
                    + "Retry in a moment or contact support if it keeps failing.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ApiErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
