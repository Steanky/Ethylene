package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefaultingConfigNodeTest {
    @Test
    void simple() {
        ConfigNode base = ConfigNode.of("key", "value");
        ConfigNode defaults = ConfigNode.of("default", 69);

        DefaultingConfigNode defaultingConfigNode = new DefaultingConfigNode(base, defaults);

        assertEquals(ConfigPrimitive.of(69), defaultingConfigNode.get("default"));
        assertEquals(2, defaultingConfigNode.size());

        base.putNumber("default", 420);

        assertEquals(ConfigPrimitive.of(420), defaultingConfigNode.get("default"));

        Set<Map.Entry<String, ConfigElement>> entrySet = new HashSet<>();
        entrySet.addAll(defaultingConfigNode.entrySet());

        assertEquals(Set.of(Map.entry("key", ConfigPrimitive.of("value")),
            Map.entry("default", ConfigPrimitive.of(420))), entrySet);
    }
}