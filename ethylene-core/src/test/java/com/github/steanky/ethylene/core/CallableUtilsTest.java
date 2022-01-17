package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.util.CallableUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CallableUtilsTest {
    @Test
    void wrapperExceptionSameType() {
        IOException original = new IOException();

        IOException resulting = assertThrows(IOException.class, () -> CallableUtils.wrapException(
                () -> {throw original;}, IOException.class, IOException::new));
        assertSame(original, resulting);
    }

    @Test
    void wrapperExceptionDifferentType() {
        Exception original = new Exception();

        IOException resulting = assertThrows(IOException.class, () -> CallableUtils.wrapException(() -> {throw original;},
                IOException.class, IOException::new));
        assertSame(original, resulting.getCause());
    }
}