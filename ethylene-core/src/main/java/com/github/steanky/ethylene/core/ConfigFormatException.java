package com.github.steanky.ethylene.core;

import java.io.IOException;

/**
 * Used to "unify" all config file parsing exceptions, so that users do not have to deal with any format-specific ones.
 * It is thrown when invalid data is encountered, such as improper format syntax. It should not be used to indicate
 * generic problems reading or writing from some IO source.
 */
public class ConfigFormatException extends IOException {
    /**
     * Creates a new ConfigParseException with the specified cause. This cause should typically be some sort of
     * format-specific exception.
     * @param throwable The {@link Throwable} that caused this exception
     */
    public ConfigFormatException(Throwable throwable) {
        super(throwable);
    }
}
