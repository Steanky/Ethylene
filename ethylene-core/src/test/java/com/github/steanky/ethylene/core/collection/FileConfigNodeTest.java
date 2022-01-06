package com.github.steanky.ethylene.core.collection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FileConfigNodeTest {
    @Test
    void ensureDirectoryFailsOnCodec() {
        FileConfigNode node = new FileConfigNode();
        assertThrows(IllegalStateException.class, node::getCodec);
    }

}