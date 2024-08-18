package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.toolkit.collection.Containers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * Common base class for all {@link ConfigNode}. Extends {@link AbstractMap}; implements {@link Object#hashCode()},
 * {@link Object#equals(Object)}, and {@link Object#toString()}. Also implements
 * {@link ConfigContainer#elementCollection()} and {@link ConfigContainer#entryCollection()}.
 */
public abstract class AbstractConfigNode extends AbstractMap<String, ConfigElement> implements ConfigNode {
    private Collection<ConfigEntry> entryCollectionView;
    private Collection<ConfigElement> elementCollectionView;

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

    @Override
    public @UnmodifiableView @NotNull Collection<ConfigEntry> entryCollection() {
        Collection<ConfigEntry> entryCollection = this.entryCollectionView;
        if (entryCollection != null) {
            return entryCollection;
        }

        this.entryCollectionView = entryCollection = Containers.mappedView(new Function<>() {
            private ConfigEntry entry;

            @Override
            public ConfigEntry apply(Entry<String, ConfigElement> mapEntry) {
                ConfigEntry entry = this.entry;
                if (entry == null) {
                    this.entry = entry = ConfigEntry.of(mapEntry.getKey(), mapEntry.getValue());
                    return entry;
                }

                entry.setKey(mapEntry.getKey());
                entry.setValue(mapEntry.getValue());
                return entry;
            }
        }, this.entrySet());
        return entryCollection;
    }

    @Override
    public @UnmodifiableView @NotNull Collection<ConfigElement> elementCollection() {
        Collection<ConfigElement> elementCollection = this.elementCollectionView;
        if (elementCollection != null) {
            return elementCollection;
        }

        this.elementCollectionView = elementCollection = Collections.unmodifiableCollection(this.values());
        return elementCollection;
    }
}
