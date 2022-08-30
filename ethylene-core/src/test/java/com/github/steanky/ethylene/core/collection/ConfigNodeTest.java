package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigNodeTest {
    @Test
    void ofTest() throws ConfigProcessException {
        ConfigNode node = ConfigNode.of("intKey", 1, "stringKey", "stringValue");
        assertEquals(1, node.getNumberOrThrow("intKey"));
        assertEquals("stringValue", node.getStringOrThrow("stringKey"));
    }

    @Test
    void throwsWhenOddLength() {
        assertThrows(IllegalArgumentException.class, () -> ConfigNode.of(new Object[1]));
    }

    @Test
    void throwsWhenNonStringKey() {
        assertThrows(IllegalArgumentException.class, () -> ConfigNode.of(new Object[] {1, "value"}));
    }
}