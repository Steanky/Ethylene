package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.codec.ConfigCodec;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileConfigNodeTest {
    private static final String KEY = "key";

    private final FileConfigNode directory;
    private final ConfigCodec mockCodec;
    private final FileConfigNode file;

    FileConfigNodeTest() {
        this.directory = new FileConfigNode();
        this.mockCodec = Mockito.mock(ConfigCodec.class);
        this.file = new FileConfigNode(mockCodec);
    }

    @Test
    void ensureDirectoryThrowsOnGetCodec() {
        assertThrows(IllegalStateException.class, directory::getCodec);
    }

    @Test
    void directoryThrowsOnNonFileElement() {
        assertThrows(IllegalArgumentException.class, () -> directory.put(KEY, Mockito.mock(ConfigElement.class)));
        assertTrue(directory.isEmpty());
    }

    @Test
    void directoryAcceptsFileElement() {
        FileConfigNode node = new FileConfigNode();
        directory.put(KEY, node);

        assertSame(node, directory.get(KEY));
    }

    @Test
    void fileHasCodec() {
        assertSame(mockCodec, file.getCodec());
    }

    @Test
    void reportsCorrectDirectoryState() {
        assertTrue(directory.isDirectory());
        assertFalse(file.isDirectory());
    }

    @Test
    void fileThrowsOnFileElement() {
        assertThrows(IllegalArgumentException.class, () -> file.put(KEY, new FileConfigNode()));
    }

    @Test
    void ensureConstructorSemantics() {
        Map<String, ConfigElement> invalidMappings = new LinkedHashMap<>();
        invalidMappings.put("element", Mockito.mock(ConfigElement.class));
        invalidMappings.put("file", new FileConfigNode());

        assertThrows(IllegalArgumentException.class, () -> new FileConfigNode(invalidMappings,
                Mockito.mock(ConfigCodec.class)));
        assertThrows(IllegalArgumentException.class, () -> new FileConfigNode(invalidMappings, null));
    }

    @Test
    void createFromMappings() {
        Map<String, ConfigElement> fileMappings = new LinkedHashMap<>();
        ConfigElement element = Mockito.mock(ConfigElement.class);
        ConfigElement element2 = Mockito.mock(ConfigElement.class);

        fileMappings.put("element", element);
        fileMappings.put("element2", element2);

        FileConfigNode node = new FileConfigNode(fileMappings, Mockito.mock(ConfigCodec.class));
        assertSame(element, node.get("element"));
        assertSame(element2, node.get("element2"));
    }
}