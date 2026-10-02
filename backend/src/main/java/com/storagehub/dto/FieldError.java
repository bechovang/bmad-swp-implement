package com.storagehub.dto;

/**
 * One entry of {@link ApiError#fieldErrors()}: the camelCase JSON path of the
 * offending field plus its human message. Shape is frozen by the FieldError
 * schema in contracts/openapi.yaml (story 1.1) - two fields, nothing else.
 */
public record FieldError(String field, String message) {
}
