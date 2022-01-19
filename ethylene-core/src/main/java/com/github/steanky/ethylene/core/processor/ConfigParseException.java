package com.github.steanky.ethylene.core.processor;

import java.io.IOException;

/**
 * Simple exception type which may be thrown by {@link ConfigProcessor} implementations to indicate invalid data.
 */
public class ConfigParseException extends IOException {
    /**
     * Constructs a new ConfigParseException with the provided message.
     * @param message a descriptive message
     */
    public ConfigParseException(String message) {
        super(message);
    }

    /**
     * Constructs a new ConfigParseException with the provided cause.
     * @param cause the {@link Throwable} responsible for this exception
     */
    public ConfigParseException(Throwable cause) {
        super(cause);
    }
}
