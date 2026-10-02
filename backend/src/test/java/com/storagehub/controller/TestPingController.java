package com.storagehub.controller;

import com.storagehub.dto.ListQuery;
import com.storagehub.dto.PageResponse;
import com.storagehub.exception.BusinessRuleException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Test-only controller (src/test, /api/v1/__test/**) able to produce every
 * failure row of the story 1.2 error matrix. No real endpoint exists yet -
 * the first one is auth in story 1.3 - so this stands in for MockMvc. No
 * service involved: handlers throw directly, exercising exactly the
 * GlobalExceptionHandler and the security chain wiring.
 */
@RestController
@RequestMapping("/api/v1/__test")
class TestPingController {

    record SampleRequest(
            @NotBlank(message = "name is required")
            @Size(min = 2, message = "name must be at least 2 characters")
            String name,

            @NotNull(message = "email is required")
            @Email(message = "email must be a valid address")
            String email,

            @NotNull(message = "count is required")
            @Min(value = 1, message = "count must be at least 1")
            Integer count) {
    }

    @GetMapping("/ping")
    Map<String, String> ping() {
        return Map.of("status", "ok");
    }

    /** Body validation: posting constraint violations here -> VALIDATION_FAILED. */
    @PostMapping("/validated")
    Map<String, String> validated(@Valid @RequestBody SampleRequest request) {
        return Map.of("name", request.name());
    }

    /** Also reads a JSON body: posting malformed JSON here -> MALFORMED_REQUEST. */
    @PostMapping("/echo")
    Map<String, String> echo(@RequestBody SampleRequest request) {
        return Map.of("name", request.name());
    }

    /** Missing required param / non-numeric value -> MALFORMED_REQUEST. */
    @GetMapping("/requires-param")
    Map<String, Integer> requiresParam(@RequestParam Integer seq) {
        return Map.of("seq", seq);
    }

    /**
     * Constraint on the parameter itself (@Min on the handler param - Spring's
     * built-in method validation) -> HandlerMethodValidationException ->
     * VALIDATION_FAILED: the exact path the ListQuery javadoc steers real
     * controllers onto.
     */
    @GetMapping("/validated-param")
    Map<String, Integer> validatedParam(
            @RequestParam @Min(value = 1, message = "seq must be at least 1") Integer seq) {
        return Map.of("seq", seq);
    }

    /** Business-rule block -> 409 with the caller-chosen code. */
    @GetMapping("/business-block")
    Map<String, String> businessBlock() {
        throw new BusinessRuleException("UNIT_UNAVAILABLE",
                "The unit is no longer available for this period. Nothing was reserved. "
                        + "Pick another unit or another date.");
    }

    /** Unexpected failure -> INTERNAL_ERROR; stack trace must stay in SLF4J. */
    @GetMapping("/boom")
    Map<String, String> boom() {
        throw new IllegalStateException("secret internal detail that must never reach the response");
    }

    /** List envelope rendering + ListQuery resolver defaults (1/25). */
    @GetMapping("/list")
    PageResponse<String> list(ListQuery query) {
        return PageResponse.of(List.of("S-3", "M-2", "M-5"), query, 3);
    }

    /** Guarded by the test-only TESTER role (TestSecurityConfig) -> 403 row. */
    @GetMapping("/role-guarded")
    Map<String, String> roleGuarded() {
        return Map.of("status", "ok");
    }
}
