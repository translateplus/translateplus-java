package io.translateplus;

import java.util.Map;

/**
 * Exception raised for insufficient credits (402).
 */
public class TranslatePlusInsufficientCreditsError extends TranslatePlusAPIError {
    public TranslatePlusInsufficientCreditsError(String message, Integer statusCode, Map<String, Object> response) {
        super(message, statusCode, response);
    }

    public TranslatePlusInsufficientCreditsError(String message, Integer statusCode) {
        super(message, statusCode);
    }
}
