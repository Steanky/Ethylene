package com.github.steanky.ethylene.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigPrimitiveTest {
    @Test
    void canMakePrimitives() {
        ConfigPrimitive primitiveString = ConfigPrimitive.of("string");
        assertTrue(primitiveString.isString());
        assertEquals("string", primitiveString.asString());

        ConfigPrimitive primitiveInt = ConfigPrimitive.of(10);
        assertTrue(primitiveInt.isNumber());
        assertEquals(10, primitiveInt.asNumber().intValue());

        ConfigPrimitive primitiveLong = ConfigPrimitive.of(10L);
        assertTrue(primitiveLong.isNumber());
        assertEquals(10L, primitiveLong.asNumber().longValue());

        ConfigPrimitive primitiveDouble = ConfigPrimitive.of(10D);
        assertTrue(primitiveDouble.isNumber());
        assertEquals(10D, primitiveDouble.asNumber().doubleValue());

        ConfigPrimitive primitiveFloat = ConfigPrimitive.of(10F);
        assertTrue(primitiveFloat.isNumber());
        assertEquals(10F, primitiveFloat.asNumber().floatValue());

        ConfigPrimitive primitiveShort = ConfigPrimitive.of((short) 10);
        assertTrue(primitiveShort.isNumber());
        assertEquals((short) 10, primitiveShort.asNumber().shortValue());

        ConfigPrimitive primitiveByte = ConfigPrimitive.of((byte) 10);
        assertTrue(primitiveByte.isNumber());
        assertEquals((byte) 10, primitiveByte.asNumber().byteValue());

        ConfigPrimitive primitiveBoolean = ConfigPrimitive.of(true);
        assertTrue(primitiveBoolean.isBoolean());
        assertTrue(primitiveBoolean.asBoolean());

        ConfigPrimitive primitiveChar = ConfigPrimitive.of('a');
        assertTrue(primitiveChar.isString());
        assertEquals("a", primitiveChar.asString());
    }

    @Test
    void failsOnUnrecognizedType() {
        assertThrows(IllegalArgumentException.class, () -> ConfigPrimitive.of(new Object()));
    }

    @Test
    void charCoercionToString() {
        ConfigPrimitive character = ConfigPrimitive.of('a');

        assertTrue(character.asScalar() instanceof Character);
        assertEquals("a", character.asString());
    }

    @Test
    void failsOnInvalidTypes() {
        ConfigPrimitive string = ConfigPrimitive.of("this is a string");
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
}