package com.hotelmanagement.exception;

/**
 * Exception thrown when a database or persistence operation encounters an error.
 */
public class DatabaseException extends ApplicationException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
