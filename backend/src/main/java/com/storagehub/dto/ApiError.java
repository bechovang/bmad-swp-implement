package com.storagehub.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * The one error envelope for every non-2xx response (AD-8), including security
 * 401/403 - shape frozen by the Error schema in contracts/openapi.yaml:
 * UPPER_SNAKE {@code code}, human {@code message} (NFR-7: what happened +
 * consequence + one next step, no exclamation marks) and {@code fieldErrors}
 * only when validation actually failed. The contract allows fieldErrors to be
 * "empty or absent" otherwise - we always emit it absent.
 */
public record ApiError(
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldError> fieldErrors) {

    public static ApiError of(ApiErrorCode code) {
        return new ApiError(code.name(), code.defaultMessage(), null);
    }

    public static ApiError of(ApiErrorCode code, List<FieldError> fieldErrors) {
        return new ApiError(code.name(), code.defaultMessage(), fieldErrors);
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }
}
