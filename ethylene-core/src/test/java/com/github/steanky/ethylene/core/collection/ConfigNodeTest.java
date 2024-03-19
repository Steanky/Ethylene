package com.github.steanky.ethylene.core.collection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigNodeTest {
    @Test
    void ofTest() {
        ConfigNode node = ConfigNode.of("intKey", 1, "stringKey", "stringValue");
        assertEquals(1, node.get("intKey").asNumber());
        assertEquals("stringValue", node.get("stringKey").asString());
    }

    @Test
    void throwsWhenOddLength() {
        assertThrows(IllegalArgumentException.class, () -> ConfigNode.of(new Object[1]));
    }

    @Test
    void throwsWhenNonStringKey() {
        assertThrows(IllegalArgumentException.class, () -> ConfigNode.of(new Object[]{1, "value"}));
    }
}