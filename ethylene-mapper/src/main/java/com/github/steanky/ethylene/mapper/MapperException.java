package com.github.steanky.ethylene.mapper;

/**
 * A general exception thrown to indicate any invalid condition during object mapping.
 */
public class MapperException extends RuntimeException {
    /**
     * Creates a new MapperException with no detail message.
     */
    public MapperException() {
        super();
    }

    /**
     * Creates a new MapperException with the provided message string.
     *
     * @param message the message string
     */
    public MapperException(String message) {
        super(message);
    }

    /**
     * Creates a new MapperException with the provided message string and cause.
     *
     * @param message the message string
     * @param cause   the cause of this exception
     */
    public MapperException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new MapperException with no detail message and the provided cause.
     *
     * @param cause the cause of this exception
     */
    public MapperException(Throwable cause) {
        super(cause);
    }
}