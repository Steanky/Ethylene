package com.github.steanky.ethylene.butylene;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class ButyleneReaderTest {
    @Test
    void test() throws IOException {
        ButyleneReader reader = new ButyleneReader(new StringReader("abc"));

        assertEquals('a', reader.peekNext());
        assertEquals('a', reader.peekNext());
        assertEquals('a', reader.next());
        assertEquals('b', reader.next());
        assertEquals('c', reader.peekNext());
        assertEquals('c', reader.peekNext());
        assertEquals('c', reader.next());
        assertEquals(-1, reader.peekNext());
        assertEquals(-1, reader.peekNext());

        assertEquals(-1, reader.next());
    }
}