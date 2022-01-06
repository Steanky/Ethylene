package com.github.steanky.ethylene.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigPrimitiveTest {
    @Test
    void canMakePrimitives() {
        ConfigPrimitive primitiveString = new ConfigPrimitive("string");
        ConfigPrimitive primitiveInt = new ConfigPrimitive(10);
        ConfigPrimitive primitiveLong = new ConfigPrimitive(10L);
        ConfigPrimitive primitiveDouble = new ConfigPrimitive(10D);
        ConfigPrimitive primitiveFloat = new ConfigPrimitive(10F);
        ConfigPrimitive primitiveShort = new ConfigPrimitive((short)10);
        ConfigPrimitive primitiveByte = new ConfigPrimitive((byte)10);
        ConfigPrimitive primitiveBoolean = new ConfigPrimitive(true);
    }

    @Test
    void failsOnUnrecognizedType() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigPrimitive(new Object()));
    }
}