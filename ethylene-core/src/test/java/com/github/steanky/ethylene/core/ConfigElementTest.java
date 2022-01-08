package com.github.steanky.ethylene.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigElementTest {
    private final ConfigElement element = new ConfigElement() {};

    @Test
    void checkDefaults() {
        assertFalse(element.isObject());
        assertFalse(element.isNull());
        assertFalse(element.isBoolean());
        assertFalse(element.isList());
        assertFalse(element.isNode());
        assertFalse(element.isNumber());
        assertFalse(element.isString());
    }

    @Test
    void checkDefaultThrows() {
        assertThrows(IllegalStateException.class, element::asObject);
        assertThrows(IllegalStateException.class, element::asBoolean);
        assertThrows(IllegalStateException.class, element::asList);
        assertThrows(IllegalStateException.class, element::asString);
        assertThrows(IllegalStateException.class, element::asNode);
        assertThrows(IllegalStateException.class, element::asNumber);
    }
}