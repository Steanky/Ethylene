package com.github.steanky.ethylene.codec.hjson;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.core.path.ConfigPath;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
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
        assertEquals("this is a value", element.at(ConfigPath.of("test_value")).asString());
        assertEquals("test", element.at(ConfigPath.of("sub/sub_string")).asString());
        assertSame(69, element.at(ConfigPath.of("another_value")).asNumber().intValue());
    }

    @Test
    void writesHjson() throws IOException {
        ConfigNode node = new LinkedConfigNode();
        node.put("test_string", ConfigPrimitive.of("value"));
        node.put("test_number", ConfigPrimitive.of(1.0));

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        codec.encode(node, stream);

        String hjson = stream.toString(Charset.defaultCharset());
        ConfigNode secondNode =
            codec.decode(new ByteArrayInputStream(hjson.getBytes(Charset.defaultCharset()))).asNode();
        assertEquals(node, secondNode);
    }
}