package com.github.steanky.ethylene.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigFormatExceptionTest {
    @Test
    void messageMatches() {
        ConfigFormatException exception = new ConfigFormatException("cause");
        assertEquals("cause", exception.getMessage());
    }

    @Test
    void causeMatches() {
        Exception cause = new Exception();
        ConfigFormatException formatException = new ConfigFormatException(cause);
        assertSame(cause, formatException.getCause());
    }
}