package io.translateplus;

/**
 * Base exception for all TranslatePlus errors.
 */
public class TranslatePlusException extends Exception {
    public TranslatePlusException(String message) {
        super(message);
    }

    public TranslatePlusException(String message, Throwable cause) {
        super(message, cause);
    }
}
