package com.github.steanky.ethylene.codec.toml;

import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.*;

class ConfigDateTest {
    private static final Date DATE = new Date(87894211525L);

    private final ConfigDate testDate;

    ConfigDateTest() {
        testDate = new ConfigDate(DATE);
    }

    @Test
    void correctType() {
        assertTrue(testDate.isObject());
        assertFalse(testDate.isBoolean());
        assertFalse(testDate.isList());
        assertFalse(testDate.isNode());
        assertFalse(testDate.isNumber());
        assertFalse(testDate.isString());
    }

    @Test
    void correctDate() {
        assertSame(DATE, testDate.getDate());
    }

    @Test
    void correctObject() {
        assertSame(DATE, testDate.asObject());
    }

    @Test
    void setChangesDate() {
        Date newDate = new Date(4789422);
        testDate.setDate(newDate);
        assertSame(newDate, testDate.getDate());
    }
}