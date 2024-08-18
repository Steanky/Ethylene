package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.Graph;
import com.github.steanky.toolkit.collection.Iterators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Internal utilities for containers. Not part of the public API.
 */
final class ConfigContainers {
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
    static @NotNull ConfigContainer copy(@NotNull ConfigContainer original) {
        if (original instanceof ImmutableView) {
            return original;
        }

        return (ConfigContainer) Graph.process(original, (ConfigElement node) -> {
            if (node instanceof ImmutableView) {
                //don't write anything to this accumulator
                return Graph.node(Iterators.iterator(), Graph.output(node, Graph.emptyAccumulator()));
            }

            ConfigContainer configContainer = node.asContainer();
            Collection<ConfigEntry> entryCollection = configContainer.entryCollection();
            ConfigContainer emptyCopy = configContainer.emptyCopy();
            if (emptyCopy == null) {
                int size = entryCollection.size();
                emptyCopy = configContainer.isNode() ? new LinkedConfigNode(size) : new ArrayConfigList(size);
            }

            return constructMutableNode(entryCollection, emptyCopy);
        }, ConfigElement::isContainer, Function.identity(), Graph.Options.TRACK_REFERENCES);
    }

    /**
     * Creates a mutable copy of the provided {@link ConfigContainer}. All nodes/lists will be reconstructed as new,
     * mutable variants, recursively.
     *
     * @param original the original container
     * @param configListCreator the function used to supply a mutable {@link ConfigList}
     * @param configNodeCreator the function used to supply a mutable {@link ConfigNode}
     * @return a mutable copy created from the original
     */
    static @NotNull ConfigContainer mutableCopy(@NotNull ConfigContainer original,
        @NotNull IntFunction<? extends ConfigNode> configNodeCreator,
        @NotNull IntFunction<? extends ConfigList> configListCreator) {
        return (ConfigContainer) Graph.process(original, (ConfigElement node) -> {
            Collection<ConfigEntry> entryCollection = node.asContainer().entryCollection();

            int size = entryCollection.size();
            ConfigContainer result = node.isNode() ? configNodeCreator.apply(size) : configListCreator.apply(size);
            if (result == null) {
                throw new NullPointerException("Container function returned null");
            }

            return constructMutableNode(entryCollection, result);
        }, ConfigElement::isContainer, Function.identity(), Graph.Options.TRACK_REFERENCES);
    }

    /**
     * Creates an immutable copy of the provided {@link ConfigContainer}.
     *
     * @param original the original container
     * @return an immutable copy of the original
     */
    static @NotNull ConfigContainer immutableCopy(@NotNull ConfigContainer original) {
        if (original instanceof Immutable) {
            return original;
        }

        return Graph.process(original, (ConfigElement node) -> {
            if (node instanceof Immutable) {
                //don't go deeper into this node, its children are obligated to be immutable
                return Graph.node(Iterators.iterator(), Graph.output(node, Graph.emptyAccumulator()));
            }

            ConfigContainer configContainer = node.asContainer();
            Collection<ConfigEntry> entryCollection = configContainer.entryCollection();

            if (entryCollection.isEmpty()) {
                ConfigContainer emptyContainer =
                    configContainer.isNode() ? EmptyImmutableConfigNode.INSTANCE : EmptyImmutableConfigList.INSTANCE;
                return Graph.node(Iterators.iterator(), Graph.output(emptyContainer, Graph.emptyAccumulator()));
            }

            int size = entryCollection.size();
            Graph.Output<ConfigElement, String> output = constructOutput(configContainer, size);

            return Graph.node(Graph.iterator(entryCollection.iterator()), output);
        }, ConfigElement::isContainer, Function.identity(), Graph.Options.TRACK_REFERENCES).asContainer();
    }

    /**
     * Produces an immutable view of the entire provided {@link ConfigContainer}. This view is read-only, but will
     * change to reflect modifications performed on the underlying container. The exact structure of the input tree will
     * be preserved.
     *
     * @param container the container to create an immutable view from
     * @return an immutable view of the provided container
     */
    static @NotNull ConfigContainer immutableView(@NotNull ConfigContainer container) {
        if (container instanceof ImmutableView) {
            return container;
        }

        return Graph.process(container, (ConfigElement node) -> {
            // all Immutable are also ImmutableView, so we don't explore those either!
            if (node instanceof ImmutableView) {
                return Graph.node(Iterators.iterator(), Graph.output(node, Graph.emptyAccumulator()));
            }

            ConfigContainer configContainer = node.asContainer();

            ConfigContainer view;
            if (configContainer.isNode()) {
                view = new ConfigNodeView(configContainer.asNode());
            } else {
                view = new ConfigListView(configContainer.asList());
            }

            return Graph.node(Graph.iterator(configContainer.entryCollection().iterator()), Graph.output(view, Graph.emptyAccumulator()));
        }, ConfigElement::isContainer, Function.identity(), Graph.Options.TRACK_REFERENCES).asContainer();
    }

    private static Graph.Node<ConfigElement, ConfigElement, String> constructMutableNode(Collection<ConfigEntry> entryCollection, ConfigContainer result) {
        ConfigNode outNode = result.isNode() ? result.asNode() : null;
        ConfigList outList = result.isList() ? result.asList() : null;
        return Graph.node(Graph.iterator(entryCollection.iterator()), Graph.output(result, (key, element, circular) -> {
            if (outNode != null) {
                outNode.put(key, element);
            } else if (outList != null) {
                outList.add(element);
            }
        }));
    }

    private static Graph.Output<ConfigElement, String> constructOutput(ConfigContainer configContainer, int size) {
        Graph.Output<ConfigElement, String> output;
        if (configContainer.isNode()) {
            Map<String, ConfigElement> underlyingMap = new LinkedHashMap<>(size, 1F);
            ConfigNode immutableNode = new ImmutableConfigNode(underlyingMap);
            output = Graph.output(immutableNode, (k, v, b) -> underlyingMap.put(k, v));
        } else {
            ConfigElement[] underlyingArray = new ConfigElement[size];
            ConfigList immutableList = new ImmutableConfigList(underlyingArray);
            output = Graph.output(immutableList, new Graph.Accumulator<>() {
                private int i;

                @Override
                public void accept(String s, ConfigElement element, boolean circular) {
                    underlyingArray[i++] = element;
                }
            });
        }

        return output;
    }

    private static class ConfigNodeView extends AbstractConfigNode implements ImmutableView {
        private final ConfigNode underlying;

        private Set<Entry<String, ConfigElement>> entrySetView;

        private ConfigNodeView(ConfigNode underlying) {
            this.underlying = underlying;
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigEntry> entryCollection() {
            return underlying.entryCollection();
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigElement> elementCollection() {
            return underlying.elementCollection();
        }

        @Override
        public int size() {
            return underlying.size();
        }

        @Override
        public boolean isEmpty() {
            return underlying.isEmpty();
        }

        @Override
        public boolean containsValue(Object value) {
            return underlying.containsValue(value);
        }

        @Override
        public boolean containsKey(Object key) {
            return underlying.containsKey(key);
        }

        @Override
        public ConfigElement get(Object key) {
            return underlying.get(key);
        }

        @NotNull
        @Override
        public Set<Entry<String, ConfigElement>> entrySet() {
            Set<Entry<String, ConfigElement>> entrySet = this.entrySetView;
            if (entrySet != null) {
                return entrySet;
            }

            this.entrySetView = entrySet = Collections.unmodifiableSet(underlying.entrySet());
            return entrySet;
        }
    }

    static final class ConfigListView extends AbstractConfigList implements RandomAccess, ImmutableView {
        private final ConfigList underlying;

        private ConfigListView(ConfigList underlying) {
            this.underlying = underlying;
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigEntry> entryCollection() {
            return underlying.entryCollection();
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigElement> elementCollection() {
            return underlying.elementCollection();
        }

        @Override
        public ConfigElement get(int index) {
            return underlying.get(index);
        }

        @Override
        public int indexOf(Object o) {
            return underlying.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return underlying.lastIndexOf(o);
        }

        @Override
        public int size() {
            return underlying.size();
        }
    }

    static final class EmptyImmutableConfigList extends AbstractConfigList implements Immutable, RandomAccess {
        static final ConfigList INSTANCE = new EmptyImmutableConfigList();

        private EmptyImmutableConfigList() {

        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigEntry> entryCollection() {
            return List.of();
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigElement> elementCollection() {
            return List.of();
        }

        @Override
        public ConfigElement get(int index) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length 0");
        }

        @Override
        public int indexOf(Object o) {
            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            return -1;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    static final class EmptyImmutableConfigNode extends AbstractConfigNode implements Immutable {
        static final ConfigNode INSTANCE = new EmptyImmutableConfigNode();

        private EmptyImmutableConfigNode() {
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigEntry> entryCollection() {
            return List.of();
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigElement> elementCollection() {
            return List.of();
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

        @Override
        public ConfigElement get(Object key) {
            return null;
        }

        @NotNull
        @Override
        public Set<String> keySet() {
            return Set.of();
        }

        @NotNull
        @Override
        public Collection<ConfigElement> values() {
            return List.of();
        }

        @NotNull
        @Override
        public Set<Entry<String, ConfigElement>> entrySet() {
            return Set.of();
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    static final class ImmutableConfigNode extends AbstractConfigNode implements Immutable {
        private final Map<String, ConfigElement> map;
        private Set<Entry<String, ConfigElement>> entrySetView;

        private boolean hashed;
        private int hashCode;

        private ImmutableConfigNode(Map<String, ConfigElement> trusted) {
            this.map = trusted;
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
        public Set<Entry<String, ConfigElement>> entrySet() {
            Set<Entry<String, ConfigElement>> entrySet = this.entrySetView;
            if (entrySet != null) {
                return entrySet;
            }

            this.entrySetView = entrySet = Collections.unmodifiableSet(map.entrySet());
            return entrySet;
        }

        @Override
        public int hashCode() {
            if (this.hashed) {
                return this.hashCode;
            }

            int hashCode = super.hashCode();

            this.hashCode = hashCode;
            VarHandle.storeStoreFence();
            this.hashed = true;

            return hashCode;
        }
    }

    static final class ImmutableConfigList extends AbstractConfigList implements Immutable, RandomAccess {
        private final ConfigElement[] elements;

        private boolean hashed;
        private int hashCode;

        private ImmutableConfigList(ConfigElement[] elements) {
            this.elements = elements;
        }

        @Override
        public ConfigElement get(int index) {
            return elements[index];
        }

        @Override
        public int size() {
            return elements.length;
        }

        @Override
        public int hashCode() {
            if (this.hashed) {
                return this.hashCode;
            }

            int hashCode = super.hashCode();

            this.hashCode = hashCode;
            VarHandle.storeStoreFence();
            this.hashed = true;

            return hashCode;
        }
    }
}