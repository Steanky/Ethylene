package com.github.steanky.ethylene.codec.json;

import com.github.steanky.ethylene.core.ConfigElement;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JsonCodecTest {
    private static final String BAD_JSON = "{ \"this json is\" : \"missing a curly bracket\"";

    private static final String GOOD_JSON = """
        {
            "top_level_string" : "string",
            "number" : 420,
            "child" : {
                "child_number" : 69,
                "child_array" : [
                    {
                        "name" : "test",
                        "value" : 69420
                    },
                    "string",
                    "another_string",
                    0.69
                ]
            }
        }
        """;

    private final JsonCodec codec = new JsonCodec();

    @Test
    void throwsFormatErrorOnBadJson() {
        assertThrows(IOException.class,
            () -> codec.decode(new ByteArrayInputStream(BAD_JSON.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void parsesCorrectJson() throws IOException {
        ConfigElement element = codec.decode(new ByteArrayInputStream(GOOD_JSON.getBytes(StandardCharsets.UTF_8)));
        assertEquals(element.getElement("top_level_string").asString(), "string");
        assertEquals(420, element.getElement("number").asNumber().intValue());
        assertTrue(element.getElement("child").isNode());
        assertEquals(69, element.getElement("child", "child_number").asNumber().intValue());
        assertTrue(element.getElement("child", "child_array").isList());
        assertTrue(element.getElement("child", "child_array", 0).isNode());
        assertEquals("test", element.getElement("child", "child_array", 0, "name").asString());
        assertEquals(69420, element.getElement("child", "child_array", 0, "value").asNumber().intValue());
        assertEquals("string", element.getElement("child", "child_array", 1).asString());
        assertEquals("another_string", element.getElement("child", "child_array", 2).asString());
        assertEquals(0.69, element.getElement("child", "child_array", 3).asNumber().doubleValue());
    }
}