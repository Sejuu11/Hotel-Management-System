package com.hotelmanagement.exception;

/**
 * Exception thrown when business validation fails.
 */
public class ValidationException extends ApplicationException {
    public ValidationException(String message) {
        super(message);
    }
}
