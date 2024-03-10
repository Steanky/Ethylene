package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultingConfigListTest {
    @Test
    void emptyBase() {
        ConfigList base = ConfigList.of();
        ConfigList defaults = ConfigList.of("test");

        DefaultingConfigList defaultingConfigList = new DefaultingConfigList(base, defaults);

        assertEquals(ConfigPrimitive.of("test"), defaultingConfigList.get(0));
        assertEquals(1, defaultingConfigList.size());

        assertThrows(IndexOutOfBoundsException.class, () -> defaultingConfigList.get(1));

        base.addString("test2");

        assertEquals(ConfigPrimitive.of("test2"), defaultingConfigList.get(0));
    }

    @Test
    void itr() {
        ConfigList base = ConfigList.of("a", "b", "c");
        ConfigList defaults = ConfigList.of("0", "1", "2", "d", "e", "f");

        DefaultingConfigList defaultingConfigList = new DefaultingConfigList(base, defaults);
        List<String> values = new ArrayList<>(6);
        for (ConfigEntry entry : defaultingConfigList.entryCollection()) {
            values.add(entry.getValue().asString());
        }

        assertEquals(List.of("a", "b", "c", "d", "e", "f"), values);
    }
}