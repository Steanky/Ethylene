package com.github.steanky.ethylene.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigPrimitiveTest {
    @Test
    void canMakePrimitives() {
        ConfigPrimitive primitiveString = new ConfigPrimitive("string");
        assertTrue(primitiveString.isString());
        assertEquals("string", primitiveString.asString());

        ConfigPrimitive primitiveInt = new ConfigPrimitive(10);
        assertTrue(primitiveInt.isNumber());
        assertEquals(10, primitiveInt.asNumber().intValue());

        ConfigPrimitive primitiveLong = new ConfigPrimitive(10L);
        assertTrue(primitiveLong.isNumber());
        assertEquals(10L, primitiveLong.asNumber().longValue());

        ConfigPrimitive primitiveDouble = new ConfigPrimitive(10D);
        assertTrue(primitiveDouble.isNumber());
        assertEquals(10D, primitiveDouble.asNumber().doubleValue());

        ConfigPrimitive primitiveFloat = new ConfigPrimitive(10F);
        assertTrue(primitiveFloat.isNumber());
        assertEquals(10F, primitiveFloat.asNumber().floatValue());

        ConfigPrimitive primitiveShort = new ConfigPrimitive((short)10);
        assertTrue(primitiveShort.isNumber());
        assertEquals((short)10, primitiveShort.asNumber().shortValue());

        ConfigPrimitive primitiveByte = new ConfigPrimitive((byte)10);
        assertTrue(primitiveByte.isNumber());
        assertEquals((byte)10, primitiveByte.asNumber().byteValue());

        ConfigPrimitive primitiveBoolean = new ConfigPrimitive(true);
        assertTrue(primitiveBoolean.isBoolean());
        assertTrue(primitiveBoolean.asBoolean());

        ConfigPrimitive primitiveChar = new ConfigPrimitive('a');
        assertTrue(primitiveChar.isString());
        assertEquals("a", primitiveChar.asString());
    }

    @Test
    void failsOnUnrecognizedType() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigPrimitive(new Object()));
    }

    @Test
    void charCoercionToString() {
        ConfigPrimitive character = new ConfigPrimitive('a');

        assertTrue(character.asScalar() instanceof Character);
        assertEquals("a", character.asString());
    }

    @Test
    void failsOnInvalidTypes() {
        ConfigPrimitive string = new ConfigPrimitive("this is a string");
        assertTrue(string.isString());
        assertTrue(string.isScalar());

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

        assertSame(null, string.asScalar());

        assertThrows(IllegalStateException.class, string::asString);
        assertThrows(IllegalStateException.class, string::asBoolean);
        assertThrows(IllegalStateException.class, string::asNumber);
        assertThrows(IllegalStateException.class, string::asList);
        assertThrows(IllegalStateException.class, string::asNode);
    }
}