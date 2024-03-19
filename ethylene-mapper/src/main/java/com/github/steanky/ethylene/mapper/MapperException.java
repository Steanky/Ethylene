package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.path.ConfigPath;

/**
 * A general exception thrown to indicate any invalid condition during object mapping.
 */
public class MapperException extends RuntimeException {
    private ConfigPath configPath;

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

    /**
     * Sets the {@link ConfigPath} associated with this exception.
     *
     * @param configPath the ConfigPath associated with this exception
     */
    public void setConfigPath(ConfigPath configPath) {
        if (this.configPath == null) {
            this.configPath = configPath;
        }
    }

    @Override
    public String getMessage() {
        String baseMessage = super.getMessage();
        StringBuilder builder = new StringBuilder(baseMessage);

        ConfigPath configPath = this.configPath;
        if (configPath != null) {
            builder.append(System.lineSeparator()).append("Error path: ").append(configPath);
        }

        return builder.toString();
    }
}