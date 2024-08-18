package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * An implementation of a "defaulting" ConfigList.
 *
 * @see ConfigList#defaulting(ConfigList, ConfigList)
 */
class DefaultingConfigList extends AbstractConfigList implements RandomAccess, ImmutableView {
    private final ConfigList base;
    private final ConfigList defaults;

    /**
     * Creates a new defaulting ConfigList.
     *
     * @param base the base list
     * @param defaults the default list, only queried if there is no value at a given index in {@code base}
     */
    DefaultingConfigList(@NotNull ConfigList base, @NotNull ConfigList defaults) {
        this.base = base;
        this.defaults = defaults;
    }

    @Override
    public ConfigElement get(int index) {
        if (index < base.size()) {
            return base.get(index);
        }

        return defaults.get(index);
    }

    @Override
    public int size() {
        return Math.max(base.size(), defaults.size());
    }
}
