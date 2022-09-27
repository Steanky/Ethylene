package com.github.steanky.ethylene.core.loader;

import com.github.steanky.ethylene.codec.json.JsonCodec;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;

class DirectoryTreeConfigSourceTest {
    @SuppressWarnings("CollectionAddedToSelf")
    @Test
    void readWriteEquivalent() throws IOException {
        ConfigNode testNode = ConfigNode.of("root",
            ConfigNode.of("children", ConfigNode.of("children", ConfigNode.of("key", "value")), "children2",
                ConfigNode.of("key2", "value2")));
        testNode.put("reference", testNode);

        JsonCodec jsonCodec = new JsonCodec();
        RegistrableCodecResolver codecResolver = new RegistrableCodecResolver();
        codecResolver.registerCodec(jsonCodec);

        Path root = Files.createTempDirectory("DirectoryTreeConfigSourceTest_output");
        DirectoryTreeConfigSource directoryTreeConfigSource =
            new DirectoryTreeConfigSource(root, codecResolver, BasicPathNameInspector.INSTANCE, jsonCodec, null, true);
        directoryTreeConfigSource.write(testNode).join();

        ConfigElement element = directoryTreeConfigSource.read().join();
        assertSame(element, element.getElement("reference"));
    }
}