package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.collection.ConfigList;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class ConfigElementTest {
    private final ConfigElement defaultElement = new ConfigElement() {};

    @Test
    void defaults() {
        assertFalse(defaultElement.isScalar());
        assertFalse(defaultElement.isNull());
        assertFalse(defaultElement.isBoolean());
        assertFalse(defaultElement.isList());
        assertFalse(defaultElement.isNode());
        assertFalse(defaultElement.isNumber());
        assertFalse(defaultElement.isString());
        assertFalse(defaultElement.isContainer());
    }

    @Test
    void defaultThrows() {
        assertThrows(IllegalStateException.class, defaultElement::asScalar);
        assertThrows(IllegalStateException.class, defaultElement::asBoolean);
        assertThrows(IllegalStateException.class, defaultElement::asList);
        assertThrows(IllegalStateException.class, defaultElement::asString);
        assertThrows(IllegalStateException.class, defaultElement::asNode);
        assertThrows(IllegalStateException.class, defaultElement::asNumber);
        assertThrows(IllegalStateException.class, defaultElement::asContainer);
    }

    @Test
    void defaultAccessNoSupplier() {
        ConfigNode mockNode = Mockito.mock(ConfigNode.class);
        ConfigList mockList = Mockito.mock(ConfigList.class);
        String pathstring = "invalid path";
        Object object = new Object();

        assertNull(defaultElement.getElementOrDefault((ConfigElement) null, pathstring));
        assertTrue(defaultElement.getBooleanOrDefault(true, pathstring));
        assertEquals(69, defaultElement.getNumberOrDefault(69, pathstring));
        assertEquals("succ", defaultElement.getStringOrDefault("succ", pathstring));
        assertSame(mockNode, defaultElement.getNodeOrDefault(mockNode, pathstring));
        assertSame(mockList, defaultElement.getListOrDefault(mockList, pathstring));
        assertSame(object, defaultElement.getObjectOrDefault(object, pathstring));
    }

    @Test
    void defaultAccessWithSupplier() {
        ConfigNode mockNode = Mockito.mock(ConfigNode.class);
        ConfigList mockList = Mockito.mock(ConfigList.class);
        Object object = new Object();

        String pathstring = "invalid path";

        assertNull(defaultElement.getElementOrDefault(() -> null, pathstring));
        assertTrue(defaultElement.getBooleanOrDefault(() -> true, pathstring));
        assertEquals(69, defaultElement.getNumberOrDefault(() -> 69, pathstring));
        assertEquals("succ", defaultElement.getStringOrDefault(() -> "succ", pathstring));
        assertSame(mockNode, defaultElement.getNodeOrDefault(() -> mockNode, pathstring));
        assertSame(mockList, defaultElement.getListOrDefault(() -> mockList, pathstring));
        assertSame(object, defaultElement.getObjectOrDefault(() -> object, pathstring));
    }

    @Test
    void getElementSpec() {
        assertSame(defaultElement, defaultElement.getElement());
        assertNull(defaultElement.getElement("invalid", "path", 420, "that doesn't exist"));
        assertNull(defaultElement.getElement(""));
        assertNull(defaultElement.getElement(0));
        assertNull(defaultElement.getElement(0, "110"));
    }
}