package com.github.steanky.ethylene.codec.yaml;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.bridge.Configuration;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class YamlCodecTest {
    private final YamlCodec codec = new YamlCodec();

    @Test
    void simpleYaml() throws IOException {
        String simpleYaml = "key: \"value\"";
        ConfigElement element = Configuration.read(simpleYaml, codec);

        assertTrue(element.isNode());
        assertEquals("value", element.asNode().get("key").asString());
    }

    @Test
    void multiDocumentYaml() throws IOException {
        String yaml = "key: \"value\"\n---\nanother_key: \"another value\"";
        ConfigElement element = Configuration.read(yaml, codec);

        assertTrue(element.isList());
        assertSame(2, element.asList().size());

        assertEquals("value", element.asList().get(0).asNode().get("key").asString());
        assertEquals("another value", element.asList().get(1).asNode().get("another_key").asString());
    }
}