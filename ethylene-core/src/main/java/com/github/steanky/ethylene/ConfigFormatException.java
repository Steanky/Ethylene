package com.github.steanky.ethylene;

import java.io.IOException;

/**
 * Used to "unify" all config file parsing exceptions, so that users do not have to deal with any format-specific ones.
 * It is thrown when invalid data is encountered, such as improper format syntax. It should not be used to indicate
 * problems reading or writing from some IO source.
 */
public class ConfigFormatException extends IOException {
    /**
     * Creates a new ConfigParseException.
     * @param throwable The {@link Throwable} that caused this exception
     */
    public ConfigFormatException(Throwable throwable) {
        super(throwable);
    }
}
