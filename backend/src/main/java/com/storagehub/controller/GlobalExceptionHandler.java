package com.storagehub.controller;

import com.storagehub.dto.ApiError;
import com.storagehub.dto.ApiErrorCode;
import com.storagehub.dto.FieldError;
import com.storagehub.exception.BusinessRuleException;
import com.storagehub.exception.InvalidCredentialsException;
import com.storagehub.exception.InvalidRequestException;
import com.storagehub.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * The one funnel turning every MVC-layer failure into the Error envelope
 * (AD-8) - no default Spring body ever reaches the client. Security 401/403
 * are handled earlier by EnvelopeAuthenticationEntryPoint /
 * EnvelopeAccessDeniedHandler, which reuse the same ApiError shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Bean Validation violated on a request body DTO -> 400 + one fieldError per field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> onValidation(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return respond(ApiErrorCode.VALIDATION_FAILED, fieldErrors);
    }

    /**
     * Constraint declared on a handler parameter itself (@RequestParam @Min(1)...)
     * - Spring's built-in method validation, not the body binder -> 400 with one
     * fieldError per offending parameter.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> onHandlerMethodValidation(HandlerMethodValidationException ex) {
        List<FieldError> fieldErrors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldError(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return respond(ApiErrorCode.VALIDATION_FAILED, fieldErrors);
    }

    /** Bean Validation on parameters (@Validated), not on a body. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> onConstraintViolation(ConstraintViolationException ex) {
        List<FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldError(
                        lastSegment(violation.getPropertyPath().toString()),
                        violation.getMessage()))
                .toList();
        return respond(ApiErrorCode.VALIDATION_FAILED, fieldErrors);
    }

    /** Unreadable JSON body -> 400 MALFORMED_REQUEST. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> onUnreadable(HttpMessageNotReadableException ex) {
        return respond(ApiErrorCode.MALFORMED_REQUEST);
    }

    /** Required request parameter missing -> 400 MALFORMED_REQUEST. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> onMissingParameter(MissingServletRequestParameterException ex) {
        return respond(ApiErrorCode.MALFORMED_REQUEST);
    }

    /** Parameter type mismatch (page=abc, seq=xyz) -> 400 MALFORMED_REQUEST. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> onTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return respond(ApiErrorCode.MALFORMED_REQUEST);
    }

    /** HTTP method not supported on the endpoint -> 405. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> onMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return respond(ApiErrorCode.METHOD_NOT_ALLOWED);
    }

    /** Request media type not supported -> 415. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> onMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return respond(ApiErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    /** Unknown path (no mapping and no static resource) -> 404. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> onNoResourceFound(NoResourceFoundException ex) {
        return respond(ApiErrorCode.NOT_FOUND);
    }

    /** Requested resource not found -> 404 NOT_FOUND. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> onResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(ApiErrorCode.NOT_FOUND.name(), ex.getMessage()));
    }

    /** Business-rule block -> 409 with the caller-chosen code and structured closing step info (Story 3.5). */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> onBusinessRule(BusinessRuleException ex) {
        return ResponseEntity.status(409).body(
                ApiError.of(ex.getCode(), ex.getMessage(), ex.getMissingStep(), ex.getStepLabel())
        );
    }

    /**
     * Cross-field / data-dependent validation raised by a service (register:
     * confirmPassword, agreeToTerms, duplicate email) -> 400 VALIDATION_FAILED
     * with the service's fieldErrors - the same envelope Bean Validation
     * produces (defer F18).
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> onInvalidRequest(InvalidRequestException ex) {
        return respond(ApiErrorCode.VALIDATION_FAILED, ex.fieldErrors());
    }

    /**
     * Login failure (unknown email / wrong password / Status 0 or 2) ->
     * 401 envelope carrying the ONE shared message; written after the
     * LOGIN_FAILED audit row (AD-5). The UNAUTHENTICATED code stays the same
     * as the security layer's so the FE has a single 401 shape.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> onInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(ApiError.of(ApiErrorCode.UNAUTHENTICATED.name(), ex.getMessage()));
    }

    /**
     * Method security (@PreAuthorize, permission matrix) denial that surfaces
     * inside MVC handling -> 403 FORBIDDEN envelope. Fixes the latent 1.2
     * gap where an AccessDeniedException fell into the catch-all and became
     * a 500; denials caught by the security chain itself still leave through
     * EnvelopeAccessDeniedHandler, rendering the identical envelope.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> onAccessDenied(AccessDeniedException ex) {
        return respond(ApiErrorCode.FORBIDDEN);
    }

    /**
     * Unauthenticated access inside MVC handling -> 401 UNAUTHENTICATED envelope.
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiError> onAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(ApiError.of(ApiErrorCode.UNAUTHENTICATED.name(), ex.getMessage()));
    }

    /**
     * Anything unexpected -> 500 INTERNAL_ERROR: generic message only - the
     * stack trace goes to SLF4J and never into the response (NFR-7).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> onUnexpected(Exception ex) {
        log.error("Unhandled exception mapped to INTERNAL_ERROR envelope", ex);
        return respond(ApiErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ApiError> respond(ApiErrorCode code) {
        return ResponseEntity.status(code.httpStatus()).body(ApiError.of(code));
    }

    private ResponseEntity<ApiError> respond(ApiErrorCode code, List<FieldError> fieldErrors) {
        return ResponseEntity.status(code.httpStatus()).body(ApiError.of(code, fieldErrors));
    }

    /** "sampleRequest.name" -> "name": the camelCase JSON path of the offending field. */
    private static String lastSegment(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot < 0 ? propertyPath : propertyPath.substring(lastDot + 1);
    }
}
