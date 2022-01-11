package com.github.steanky.ethylene.codec.toml;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TomlCodecTest {
    private static final String BAD_TOML = "illegal++++fasfsf\n== toml (this is not valid at all)";
    private final TomlCodec codec = new TomlCodec();

    @Test
    void throwsFormatErrorOnBadToml() {
        assertThrows(IOException.class, () -> codec.decode(
                new ByteArrayInputStream(BAD_TOML.getBytes(StandardCharsets.UTF_8))));
    }
}