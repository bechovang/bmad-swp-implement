package com.storagehub.exception;

import com.storagehub.dto.FieldError;

import java.util.List;

/**
 * A request that is structurally readable but fails cross-field or
 * data-dependent validation only the service layer can judge (register:
 * password confirmation, terms not accepted, duplicate email). Rendered by
 * GlobalExceptionHandler as 400 VALIDATION_FAILED with these fieldErrors -
 * same envelope as Bean Validation, raised from the service (defer F18).
 */
public class InvalidRequestException extends RuntimeException {

    private final List<FieldError> fieldErrors;

    public InvalidRequestException(List<FieldError> fieldErrors) {
        super("Request failed service-level validation");
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldError> fieldErrors() {
        return fieldErrors;
    }
}
