package com.storagehub.exception;

import java.util.regex.Pattern;

/**
 * A business rule blocked the operation (AD-8): the handler maps it to 409
 * CONFLICT with the caller-chosen machine {@code code} (e.g. UNIT_UNAVAILABLE,
 * SHIFT_CONFLICT - must match the openapi pattern ^[A-Z][A-Z0-9_]*$). This is
 * the service-to-HTTP contract: services throw it with the code declared on
 * their operation in contracts/openapi.yaml and an NFR-7 message (what
 * happened + consequence + one next step). A code that breaks the pattern is
 * refused at construction - it would silently ship an envelope violating the
 * Error schema.
 */
public class BusinessRuleException extends RuntimeException {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Business rule code must match ^[A-Z][A-Z0-9_]*$ (openapi Error.code), got: " + code);
        }
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
