package io.translateplus;

/**
 * Base exception for all TranslatePlus errors.
 * Extends RuntimeException to allow throwing from constructors and methods without declaring throws.
 */
public class TranslatePlusException extends RuntimeException {
    public TranslatePlusException(String message) {
        super(message);
    }

    public TranslatePlusException(String message, Throwable cause) {
        super(message, cause);
    }
}
