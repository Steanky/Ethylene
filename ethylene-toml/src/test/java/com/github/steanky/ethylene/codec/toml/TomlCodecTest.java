package com.github.steanky.ethylene.codec.toml;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.moandjiezana.toml.TomlWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TomlCodecTest {
    private static final String BAD_TOML = "illegal++++fasfsf\n== toml (this is not valid at all)";
    private final TomlCodec codec = new TomlCodec(new TomlWriter());

    @Test
    void throwsFormatErrorOnBadToml() {
        assertThrows(IOException.class, () -> codec.decode(
                new ByteArrayInputStream(BAD_TOML.getBytes(StandardCharsets.UTF_8))));
    }
}