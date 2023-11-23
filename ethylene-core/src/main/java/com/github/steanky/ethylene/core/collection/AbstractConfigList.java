package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;

import java.util.AbstractList;
import java.util.List;

/**
 * Common base class for all {@link ConfigList}. Extends {@link AbstractList}; implements {@link Object#hashCode()},
 * {@link Object#equals(Object)}, and {@link Object#toString()}.
 */
public abstract class AbstractConfigList extends AbstractList<ConfigElement> implements ConfigList {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof List<?>)) {
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
