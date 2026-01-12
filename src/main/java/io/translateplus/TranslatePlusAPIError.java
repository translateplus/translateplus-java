package io.translateplus;

import java.util.Map;

/**
 * Exception raised for API errors.
 */
public class TranslatePlusAPIError extends TranslatePlusException {
    private final Integer statusCode;
    private final Map<String, Object> response;

    public TranslatePlusAPIError(String message, Integer statusCode, Map<String, Object> response) {
        super(message);
        this.statusCode = statusCode;
        this.response = response;
    }

    public TranslatePlusAPIError(String message, Integer statusCode) {
        this(message, statusCode, null);
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Map<String, Object> getResponse() {
        return response;
    }
}
