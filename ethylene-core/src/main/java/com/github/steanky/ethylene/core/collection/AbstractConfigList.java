package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;

import java.util.AbstractList;
import java.util.List;

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
}
