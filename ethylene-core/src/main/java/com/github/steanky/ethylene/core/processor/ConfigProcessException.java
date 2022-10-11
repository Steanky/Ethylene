package com.github.steanky.ethylene.core.processor;

import java.io.IOException;

/**
 * Simple exception type which may be thrown by {@link ConfigProcessor} implementations to indicate invalid data.
 */
public class ConfigProcessException extends IOException {
    /**
     * Constructs a new ConfigProcessException with the provided message.
     *
     * @param message a descriptive message
     */
    public ConfigProcessException(String message) {
        super(message);
    }

    /**
     * Constructs a new ConfigProcessException with the provided cause.
     *
     * @param cause the {@link Throwable} responsible for this exception
     */
    public ConfigProcessException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new ConfigProcessException with the provided message and cause.
     *
     * @param message a descriptive message
     * @param cause   the {@link Throwable} responsible for this exception
     */
    public ConfigProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
