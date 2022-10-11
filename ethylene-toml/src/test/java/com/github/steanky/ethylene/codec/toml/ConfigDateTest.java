package com.github.steanky.ethylene.codec.toml;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.Temporal;

import static org.junit.jupiter.api.Assertions.*;

class ConfigDateTest {
    private static final Temporal DATE = Instant.ofEpochMilli(87894211525L);

    private final ConfigDate testDate;

    ConfigDateTest() {
        testDate = new ConfigDate(DATE);
    }

    @Test
    void correctType() {
        assertTrue(testDate.isScalar());
        assertTrue(testDate.isString());
        assertFalse(testDate.isBoolean());
        assertFalse(testDate.isList());
        assertFalse(testDate.isNode());
        assertFalse(testDate.isNumber());
    }

    @Test
    void correctDate() {
        assertSame(DATE, testDate.getTemporal());
    }

    @Test
    void correctObject() {
        assertSame(DATE, testDate.asScalar());
    }
}