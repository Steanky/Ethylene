package com.github.steanky.ethylene.codec.json;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonCodecTest {
    private static final String BAD_JSON = "{ \"this json is\" : \"missing a curly bracket\"";

    private final JsonCodec codec = new JsonCodec();

    @Test
    void throwsFormatErrorOnBadJson() {
        assertThrows(IOException.class, () -> codec.decode(
                new ByteArrayInputStream(BAD_JSON.getBytes(StandardCharsets.UTF_8))));
    }
}