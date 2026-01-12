package io.translateplus;

import java.util.Map;

/**
 * Exception raised for authentication errors (401, 403).
 */
public class TranslatePlusAuthenticationError extends TranslatePlusAPIError {
    public TranslatePlusAuthenticationError(String message, Integer statusCode, Map<String, Object> response) {
        super(message, statusCode, response);
    }

    public TranslatePlusAuthenticationError(String message, Integer statusCode) {
        super(message, statusCode);
    }
}
