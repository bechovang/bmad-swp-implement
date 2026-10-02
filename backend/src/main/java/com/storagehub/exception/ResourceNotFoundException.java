package com.storagehub.exception;

/**
 * Thrown when a requested resource is not found (maps to 404 NOT_FOUND).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(resourceName + " not found with id: " + identifier);
    }
}
