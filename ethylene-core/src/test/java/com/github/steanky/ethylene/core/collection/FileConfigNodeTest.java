package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class FileConfigNodeTest {
    private final FileConfigNode directory;
    private final FileConfigNode file;

    FileConfigNodeTest() {
        this.directory = new FileConfigNode();
        this.file = new FileConfigNode();
    }

    @Test
    void ensureDirectoryThrowsOnGetCodec() {
        assertThrows(IllegalStateException.class, directory::getCodec);
    }

    @Test
    void directoryThrowsOnNonFileElement() {
        assertThrows(IllegalArgumentException.class, () -> directory.put("key", Mockito.mock(ConfigElement.class)));
        assertTrue(directory.isEmpty());
    }

    @Test
    void directoryAcceptsFileElement() {
        FileConfigNode node = new FileConfigNode();
        directory.put("key", node);

        assertSame(node, directory.get("key"));
    }
}