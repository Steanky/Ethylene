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

    @Test
    void caching() {
        ConfigPrimitive cacheEmpty = ConfigPrimitive.of("");
        ConfigPrimitive other = ConfigPrimitive.of("");
        assertSame(cacheEmpty, other);

        for (int i = -128; i <= 127; i++) {
            ConfigPrimitive firstLong = ConfigPrimitive.of((long)i);
            ConfigPrimitive secondLong = ConfigPrimitive.of((long)i);

            assertSame(firstLong, secondLong);
            assertEquals(firstLong.asNumber(), (long)i);

            ConfigPrimitive firstInteger = ConfigPrimitive.of(i);
            ConfigPrimitive secondInteger = ConfigPrimitive.of(i);

            assertSame(firstInteger, secondInteger);
            assertEquals(firstInteger.asNumber(), i);

            assertSame(firstInteger, secondInteger);
            assertEquals(firstInteger.asNumber(), i);

            ConfigPrimitive firstShort = ConfigPrimitive.of((short)i);
            ConfigPrimitive secondShort = ConfigPrimitive.of((short)i);

            assertSame(firstShort, secondShort);
            assertEquals(firstShort.asNumber(), (short)i);

            ConfigPrimitive firstByte = ConfigPrimitive.of((byte) i);
            ConfigPrimitive secondByte = ConfigPrimitive.of((byte) i);

            assertSame(firstByte, secondByte);
            assertEquals(firstByte.asNumber(), (byte)i);
        }

        for (int i = 0; i <= 127; i++) {
            ConfigPrimitive firstChar = ConfigPrimitive.of((char) i);
            ConfigPrimitive secondChar = ConfigPrimitive.of((char)i);

            assertSame(firstChar, secondChar);
            assertEquals(firstChar.asString(), Character.toString((char)i));
        }
    }
}