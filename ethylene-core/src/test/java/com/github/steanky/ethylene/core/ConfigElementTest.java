package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.path.ConfigPath;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigElementTest {
    private final ConfigElement defaultElement = new ConfigElement() {
        private static final Object O = new Object();
        @Override
        public @NotNull ElementType type() {
            return ElementType.SCALAR;
        }

        @Override
        public Object asScalar() {
            return O;
        }
    };

    @Test
    void defaults() {
        assertTrue(defaultElement.isScalar());
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
        assertDoesNotThrow(defaultElement::asScalar);
        assertThrows(IllegalStateException.class, defaultElement::asBoolean);
        assertThrows(IllegalStateException.class, defaultElement::asList);
        assertThrows(IllegalStateException.class, defaultElement::asString);
        assertThrows(IllegalStateException.class, defaultElement::asNode);
        assertThrows(IllegalStateException.class, defaultElement::asNumber);
        assertThrows(IllegalStateException.class, defaultElement::asContainer);
    }

    @Test
    void getElementSpec() {
        assertSame(defaultElement, defaultElement.at(ConfigPath.EMPTY));
        assertNull(defaultElement.at(ConfigPath.of("invalid/path/that/doesn't/exist")));
    }
}