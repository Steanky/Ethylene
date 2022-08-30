package com.github.steanky.ethylene.codec.toml;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigList;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TomlCodecTest {
    private static final String BAD_TOML = "illegal++++fasfsf\n== toml (this is not valid at all)";
    private static final String SIMPLE_TOML = "value = \"this is a string\"";

    private static final String COMPLEX_TOML = """
            topLevelString = "topLevel"
            topLevelInt = 69
            intArray = [ 0, 1, 2, 3, 4 ]
                        
            [[object_array]]
            x = 0
            y = 1
            z = 2
                        
            [[object_array]]
            x = 3
            y = 4
            z = 5
                        
            [[object_array]]
            x = 6
            y = 7
            z = 8
            """;
    private final TomlCodec codec = new TomlCodec();

    @Test
    void throwsFormatErrorOnBadToml() {
        assertThrows(IOException.class,
                () -> codec.decode(new ByteArrayInputStream(BAD_TOML.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void readsSimpleToml() throws IOException {
        ConfigElement element = codec.decode(new ByteArrayInputStream(SIMPLE_TOML.getBytes(Charset.defaultCharset())));
        assertEquals("this is a string", element.getStringOrThrow("value"));
    }

    @Test
    void readsComplexToml() throws IOException {
        ConfigElement element = codec.decode(new ByteArrayInputStream(COMPLEX_TOML.getBytes(Charset.defaultCharset())));
        ConfigList list = element.getListOrThrow("object_array");
        assertEquals("topLevel", element.getStringOrThrow("topLevelString"));
        assertEquals(3, list.size());
    }
}