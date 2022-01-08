package com.github.steanky.ethylene.codec.toml;

import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TomlCodecTest {
    private static final String BAD_TOML = "illegal++++fasfsf\n== toml (this is not valid at all)";
    private final TomlCodec codec = new TomlCodec();

    @Test
    void throwsFormatErrorOnBadToml() {
        assertThrows(IOException.class, () -> codec.decodeNode(
                new ByteArrayInputStream(BAD_TOML.getBytes(StandardCharsets.UTF_8)), LinkedConfigNode::new));
    }
}