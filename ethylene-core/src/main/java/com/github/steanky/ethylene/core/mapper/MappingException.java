package com.github.steanky.ethylene.core.mapper;

/**
 * Indicates an exception that occurred during object mapping.
 */
public class MappingException extends RuntimeException {
    /**
     * Constructs a new MappingException with the provided message.
     * @param message a descriptive message
     */
    public MappingException(String message) {
        super(message);
    }

    /**
     * Constructs a new MappingException with the provided cause.
     * @param cause the {@link Throwable} responsible for this exception
     */
    public MappingException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new MappingException with the provided message and cause.
     * @param message a descriptive message
     * @param cause the {@link Throwable} responsible for this exception
     */
    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
