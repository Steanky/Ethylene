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
        ConfigPrimitive primitiveChar = new ConfigPrimitive('a');
    }

    @Test
    void failsOnUnrecognizedType() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigPrimitive(new Object()));
    }

    @Test
    void charCoercionToString() {
        ConfigPrimitive character = new ConfigPrimitive('a');

        assertTrue(character.asObject() instanceof Character);
        assertEquals("a", character.asString());
    }

    @Test
    void failsOnInvalidTypes() {
        ConfigPrimitive string = new ConfigPrimitive("this is a string");
        assertTrue(string.isString());
        assertTrue(string.isObject());

        assertFalse(string.isBoolean());
        assertFalse(string.isNumber());
        assertFalse(string.isList());
        assertFalse(string.isNode());
        assertFalse(string.isNull());

        assertThrows(IllegalStateException.class, string::asBoolean);
        assertThrows(IllegalStateException.class, string::asNumber);
        assertThrows(IllegalStateException.class, string::asList);
        assertThrows(IllegalStateException.class, string::asNode);
    }

    @Test
    void setObjectChangesType() {
        ConfigPrimitive string = new ConfigPrimitive("this is a string");
        string.setObject(null);

        assertTrue(string.isNull());

        assertSame(null, string.asObject());

        assertThrows(IllegalStateException.class, string::asString);
        assertThrows(IllegalStateException.class, string::asBoolean);
        assertThrows(IllegalStateException.class, string::asNumber);
        assertThrows(IllegalStateException.class, string::asList);
        assertThrows(IllegalStateException.class, string::asNode);
    }
}