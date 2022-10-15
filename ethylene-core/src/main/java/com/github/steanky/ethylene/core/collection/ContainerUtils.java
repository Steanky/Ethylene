package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.Graph;
import com.github.steanky.toolkit.collection.Iterators;
import com.github.steanky.toolkit.function.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.function.Function;

/**
 * Internal utilities for collections. Not part of the public API.
 */
final class ContainerUtils {
    /**
     * Deep-copies the provided {@link ConfigContainer}, maintaining the extract structure of the input tree, including
     * circular references, and the implementation types of every container encountered (when possible).
     * <p>
     * Immutable implementations of {@link ConfigContainer} will not be copied; the same instances will exist in the
     * input as well as the output tree.
     *
     * @param original the original
     * @return an exact copy of the input
     */
    static @NotNull ConfigContainer clone(@NotNull ConfigContainer original) {
        return (ConfigContainer) Graph.process(original, (ConfigElement node) -> {
            ConfigContainer configContainer = node.asContainer();
            Collection<ConfigEntry> entryCollection = configContainer.entryCollection();
            if (configContainer instanceof Immutable) {
                //don't write anything to this accumulator
                return Graph.node(entryCollection.iterator(), Graph.output(configContainer, Graph
                    .emptyAccumulator()));
            }

            ConfigContainer result;
            try {
                //use the implementation's copy method...
                result = configContainer.emptyCopy();
            }
            catch (UnsupportedOperationException e) {
                //...unless we can't due to it not being supported, in which case use reasonable defaults
                int size = entryCollection.size();
                result = configContainer.isNode() ? new LinkedConfigNode(size) : new ArrayConfigList(size);
            }

            ConfigContainer out = result;
            return Graph.node(entryCollection.iterator(), Graph.output(out, (key, element, circular) -> {
                if (out.isNode()) {
                    out.asNode().put(key, element);
                }
                else {
                    out.asList().add(element);
                }
            }));
        }, ConfigElement::isContainer, Function.identity(), Graph.Options.TRACK_REFERENCES);
    }

    /**
     * Creates an immutable copy of the provided {@link ConfigContainer}.
     *
     * @param original the original container
     * @return an immutable copy of the original
     */
    static @NotNull ConfigContainer immutableCopy(@NotNull ConfigContainer original) {
        return Graph.process(original, (ConfigElement node) -> {
            ConfigContainer configContainer = node.asContainer();
            Collection<ConfigEntry> entryCollection = configContainer.entryCollection();

            if (entryCollection.isEmpty()) {
                ConfigContainer emptyContainer = configContainer.isNode() ? EmptyImmutableConfigNode.INSTANCE :
                    EmptyImmutableConfigList.INSTANCE;

                return Graph.node(Iterators.iterator(), Graph.output(Wrapper.of(emptyContainer),
                    Graph.emptyAccumulator()));
            }

            int size = entryCollection.size();
            Wrapper<ConfigElement> result = Wrapper.ofNull();
            Graph.Output<Wrapper<ConfigElement>, String> output;
            if (configContainer.isNode()) {
                ConfigEntry[] entries = new ConfigEntry[size];
                output = Graph.output(result, new Graph.Accumulator<>() {
                    private int i;

                    @Override
                    public void accept(String s, Wrapper<ConfigElement> configElementWrapper, boolean visited) {
                        entries[i++] = ConfigEntry.of(s, configElementWrapper.get());

                        if (i == size) {
                            result.set(new ImmutableConfigNode(entries));
                        }
                    }
                });
            }
            else {
                ConfigElement[] elements = new ConfigElement[size];
                output = Graph.output(result, new Graph.Accumulator<>() {
                    private int i;
                    @Override
                    public void accept(String s, Wrapper<ConfigElement> configElementWrapper, boolean visited) {
                        elements[i++] = configElementWrapper.get();

                        if (i == size) {
                            result.set(new ImmutableConfigList(elements));
                        }
                    }
                });
            }

            return Graph.node(entryCollection.iterator(), output);
        }, ConfigElement::isContainer, Wrapper::of, Graph.Options.TRACK_REFERENCES | Graph.Options
            .LAZY_ACCUMULATION | Graph.Options.DEPTH_FIRST).get().asContainer();
    }

    private static class EmptyImmutableConfigList extends AbstractList<ConfigElement> implements ConfigList, Immutable {
        private static final ConfigList INSTANCE = new EmptyImmutableConfigList();

        private EmptyImmutableConfigList() {

        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigEntry> entryCollection() {
            return Collections.emptyList();
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigElement> elementCollection() {
            return Collections.emptyList();
        }

        @Override
        public ConfigElement get(int index) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length 0");
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public int indexOf(Object o) {
            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            return -1;
        }
    }

    private static class EmptyImmutableConfigNode extends AbstractMap<String, ConfigElement> implements ConfigNode,
        Immutable {
        private static final ConfigNode INSTANCE = new EmptyImmutableConfigNode();

        private EmptyImmutableConfigNode() {}

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigEntry> entryCollection() {
            return Collections.emptyList();
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigElement> elementCollection() {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public Set<Entry<String, ConfigElement>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @NotNull
        @Override
        public Set<String> keySet() {
            return Collections.emptySet();
        }

        @NotNull
        @Override
        public Collection<ConfigElement> values() {
            return Collections.emptyList();
        }
    }

    private static class ImmutableConfigNode extends AbstractMap<String, ConfigElement> implements ConfigNode,
        Immutable {
        private final Map<String, ConfigElement> map;

        private ImmutableConfigNode(ConfigEntry[] entries) {
            this.map = Map.ofEntries(entries);
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigEntry> entryCollection() {
            return new AbstractCollection<>() {
                @Override
                public Iterator<ConfigEntry> iterator() {
                    return new Iterator<>() {
                        private final Iterator<Map.Entry<String, ConfigElement>> entryIterator = map.entrySet()
                            .iterator();

                        @Override
                        public boolean hasNext() {
                            return entryIterator.hasNext();
                        }

                        @Override
                        public ConfigEntry next() {
                            Map.Entry<String, ConfigElement> next = entryIterator.next();
                            return ConfigEntry.of(next.getKey(), next.getValue());
                        }
                    };
                }

                @Override
                public int size() {
                    return map.size();
                }
            };
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean containsValue(Object value) {
            return map.containsValue(value);
        }

        @Override
        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        @Override
        public ConfigElement get(Object key) {
            return map.get(key);
        }

        @NotNull
        @Override
        public Set<String> keySet() {
            return map.keySet();
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigElement> elementCollection() {
            return map.values();
        }

        @NotNull
        @Override
        public Set<Entry<String, ConfigElement>> entrySet() {
            return map.entrySet();
        }
    }

    private static class ImmutableConfigList extends AbstractList<ConfigElement> implements ConfigList, Immutable {
        private final ConfigElement[] elements;

        private ImmutableConfigList(ConfigElement[] elements) {
            this.elements = Arrays.copyOf(elements, elements.length);
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigEntry> entryCollection() {
            return new AbstractCollection<>() {
                @Override
                public Iterator<ConfigEntry> iterator() {
                    return new Iterator<>() {
                        private int i;

                        @Override
                        public boolean hasNext() {
                            return i < elements.length;
                        }

                        @Override
                        public ConfigEntry next() {
                            return ConfigEntry.of(elements[i++]);
                        }
                    };
                }

                @Override
                public int size() {
                    return elements.length;
                }
            };
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigElement> elementCollection() {
            return Iterators.arrayView(elements);
        }

        @Override
        public ConfigElement get(int index) {
            return elements[index];
        }

        @Override
        public int size() {
            return elements.length;
        }
    }
}
