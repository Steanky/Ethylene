package com.github.steanky.ethylene.mapper.type;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GenericInfoTest {
    @Test
    void emptyTypeArrayNullMetadata() {
        byte[] expected = new byte[]{0, 0, 0};
        assertArrayEquals(expected, GenericInfo.identifier((byte) 0));
    }

    @Test
    void emptyTypeArrayNonNullSingleCharacterMetadata() {
        byte[] expected = new byte[]{0, 0, 97};
        assertArrayEquals(expected, GenericInfo.identifier((byte) 0, "a"));
    }

    @Test
    void emptyTypeArrayNonNullMultiCharacterMetadata() {
        byte[] expected = new byte[]{0, 0, 97, 98, 99};
        assertArrayEquals(expected, GenericInfo.identifier((byte) 0, "abc"));
    }

    @Test
    void variousTypes() {
        for (int i = 0; i < 255; i++) {
            byte[] expected = new byte[]{(byte) i, 0, 0};
            assertArrayEquals(expected, GenericInfo.identifier((byte) i));
        }
    }

    @Test
    void singleClassType() {
        byte[] identifier = GenericInfo.identifier((byte) 0, List.class);
        String id = GenericInfo.CHARSET.decode(ByteBuffer.wrap(identifier, 1, 14)).toString();
        assertEquals("java.util.List", id);
    }

    @Test
    void multiClassType() {
        byte[] expected = new byte[33];
        byte[] list = GenericInfo.CHARSET.encode("java.util.List").array();
        byte[] string = GenericInfo.CHARSET.encode("java.lang.String").array();

        System.arraycopy(list, 0, expected, 0, list.length);
        expected[list.length] = 0;
        System.arraycopy(string, 0, expected, list.length + 1, string.length);

        byte[] identifier = GenericInfo.identifier((byte) 0, List.class, String.class);
        String expectedString = GenericInfo.CHARSET.decode(ByteBuffer.wrap(expected)).toString();
        String actualString = GenericInfo.CHARSET.decode(ByteBuffer.wrap(identifier, 1, 33)).toString();

        assertEquals(expectedString, actualString);
    }
}