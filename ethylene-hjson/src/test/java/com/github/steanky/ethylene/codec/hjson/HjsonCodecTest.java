package com.github.steanky.ethylene.codec.hjson;

import com.github.steanky.ethylene.core.ConfigElement;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HjsonCodecTest {
    private static final String TEST_HJSON = """
            {
                #this is a comment
                test_value: "this is a value"
                another_value: 69
                array: [ "one", "two", "three" ]
                sub: {
                    sub_string: "test"
                }
            }""";

    private final HjsonCodec codec = new HjsonCodec();

    @Test
    void parsesHjson() throws IOException {
        ConfigElement element = codec.decode(new ByteArrayInputStream(TEST_HJSON.getBytes(StandardCharsets.UTF_8)));
        assertTrue(element.isNode());
        assertEquals("this is a value", element.getElement("test_value").asString());
        assertEquals("test", element.getElement("sub", "sub_string").asString());
        assertSame(69, element.getElement("another_value").asNumber().intValue());
    }
}