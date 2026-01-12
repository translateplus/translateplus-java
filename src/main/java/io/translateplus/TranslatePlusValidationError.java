package io.translateplus;

/**
 * Exception raised for validation errors.
 */
public class TranslatePlusValidationError extends TranslatePlusException {
    public TranslatePlusValidationError(String message) {
        super(message);
    }
}
