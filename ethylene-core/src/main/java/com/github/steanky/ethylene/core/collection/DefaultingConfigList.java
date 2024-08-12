package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.toolkit.collection.Containers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.RandomAccess;

/**
 * An implementation of a "defaulting" ConfigList.
 *
 * @see ConfigList#defaulting(ConfigList, ConfigList)
 */
class DefaultingConfigList extends AbstractConfigList implements RandomAccess {
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
    public @UnmodifiableView @NotNull Collection<ConfigEntry> entryCollection() {
        return Containers.mappedView(ConfigEntry::of, elementCollection());
    }

    @Override
    public @UnmodifiableView @NotNull Collection<ConfigElement> elementCollection() {
        return new AbstractCollection<>() {
            @Override
            public @NotNull Iterator<ConfigElement> iterator() {
                int baseSize = base.size();
                int max = DefaultingConfigList.this.size();

                return new Iterator<>() {
                    private int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < max;
                    }

                    @Override
                    public ConfigElement next() {
                        int i = this.i++;
                        if (i < baseSize) {
                            return base.get(i);
                        }

                        return defaults.get(i);
                    }
                };
            }

            @Override
            public int size() {
                return DefaultingConfigList.this.size();
            }
        };
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
