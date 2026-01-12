package io.translateplus;

import java.util.Map;

/**
 * Exception raised for rate limit errors (429).
 */
public class TranslatePlusRateLimitError extends TranslatePlusAPIError {
    public TranslatePlusRateLimitError(String message, Integer statusCode, Map<String, Object> response) {
        super(message, statusCode, response);
    }

    public TranslatePlusRateLimitError(String message, Integer statusCode) {
        super(message, statusCode);
    }
}
