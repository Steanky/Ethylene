package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.toolkit.collection.Containers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;

/**
 * An implementation of a "defaulting" ConfigNode.
 *
 * @see ConfigNode#defaulting(ConfigNode, ConfigNode)
 */
class DefaultingConfigNode extends AbstractConfigNode implements ImmutableView {
    private final ConfigNode base;
    private final ConfigNode defaults;

    private Set<Entry<String, ConfigElement>> entrySetView;

    /**
     * Constructs a new defaulting ConfigNode.
     *
     * @param base the base node, which is queried first
     * @param defaults the default node, which is only queried if a requested value is not present in {@code base}
     */
    DefaultingConfigNode(@NotNull ConfigNode base, @NotNull ConfigNode defaults) {
        this.base = base;
        this.defaults = defaults;
    }

    @NotNull
    @Override
    public Set<Entry<String, ConfigElement>> entrySet() {
        Set<Entry<String, ConfigElement>> entrySet = this.entrySetView;
        if (entrySet != null) {
            return entrySet;
        }

        this.entrySetView = entrySet = new AbstractSet<>() {
            @Override
            public @NotNull Iterator<Map.Entry<String, ConfigElement>> iterator() {
                return new Iterator<>() {
                    private final Iterator<ConfigEntry> baseIterator = base.entryCollection().iterator();
                    private final Iterator<ConfigEntry> defaultsIterator = defaults.entryCollection().iterator();

                    private ConfigEntry nextDefault;

                    @Override
                    public boolean hasNext() {
                        return baseIterator.hasNext() || tryAdvanceDefaults();
                    }

                    @Override
                    public Map.Entry<String, ConfigElement> next() {
                        if (baseIterator.hasNext()) {
                            return baseIterator.next();
                        }

                        ConfigEntry nextDefault = this.nextDefault;
                        if (nextDefault != null) {
                            this.nextDefault = null;
                            return nextDefault;
                        }

                        if (tryAdvanceDefaults()) {
                            nextDefault = this.nextDefault;
                            this.nextDefault = null;
                            return nextDefault;
                        }

                        throw new NoSuchElementException();
                    }

                    private boolean tryAdvanceDefaults() {
                        while (defaultsIterator.hasNext()) {
                            ConfigEntry nextDefault = defaultsIterator.next();

                            if (!base.containsKey(nextDefault.getKey())) {
                                this.nextDefault = nextDefault;
                                return true;
                            }
                        }

                        return false;
                    }
                };
            }

            @Override
            public int size() {
                return DefaultingConfigNode.this.size();
            }

            @Override
            public boolean contains(Object o) {
                return base.entrySet().contains(o) || defaults.entrySet().contains(o);
            }
        };

        return entrySet;
    }

    @Override
    public ConfigElement get(Object key) {
        ConfigElement element = base.get(key);
        if (element != null) {
            return element;
        }

        return defaults.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return base.containsKey(key) || defaults.containsKey(key);
    }

    @Override
    public int size() {
        int size = base.size();
        for (String key : defaults.keySet()) {
            if (!base.containsKey(key)) {
                size++;
            }
        }

        return size;
    }

    @Override
    public boolean isEmpty() {
        return base.isEmpty() && defaults.isEmpty();
    }
}