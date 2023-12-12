package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;

import java.util.AbstractMap;
import java.util.Map;

/**
 * Common base class for all {@link ConfigNode}. Extends {@link AbstractMap}; implements {@link Object#hashCode()},
 * {@link Object#equals(Object)}, and {@link Object#toString()}.
 */
public abstract class AbstractConfigNode extends AbstractMap<String, ConfigElement> implements ConfigNode {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Map<?, ?>)) {
            return false;
        }

        return ConfigElements.equals(this, o);
    }

    @Override
    public int hashCode() {
        return ConfigElements.hashCode(this);
    }

    @Override
    public String toString() {
        return ConfigElements.toString(this);
    }
}
