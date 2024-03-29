package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

/**
 * Internal utilities for ConfigElements. Not part of the public API, but is public to enable cross-package access.
 */
@ApiStatus.Internal
public final class ConfigElements {
    private ConfigElements() {
        throw new AssertionError("Why?");
    }

    private static class LazyListContainer extends AbstractList<ConfigElement> implements ConfigList {
        private final List<?> list;
        private final Collection<ConfigEntry> entryCollection;
        private final Collection<ConfigElement> elementCollection;

        private LazyListContainer(List<?> list) {
            this.list = list;
            this.entryCollection = new AbstractCollection<>() {
                @Override
                public Iterator<ConfigEntry> iterator() {
                    Iterator<?> backing = list.iterator();

                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return backing.hasNext();
                        }

                        @Override
                        public ConfigEntry next() {
                            ConfigElement element = maybeWrap(backing.next());
                            if (element == null) {
                                return null;
                            }

                            return ConfigEntry.of(element);
                        }
                    };
                }

                @Override
                public int size() {
                    return list.size();
                }
            };
            this.elementCollection = new AbstractCollection<>() {
                @Override
                public Iterator<ConfigElement> iterator() {
                    Iterator<?> backing = list.iterator();

                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return backing.hasNext();
                        }

                        @Override
                        public ConfigElement next() {
                            return maybeWrap(backing.next());
                        }
                    };
                }

                @Override
                public int size() {
                    return list.size();
                }
            };
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigEntry> entryCollection() {
            return entryCollection;
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigElement> elementCollection() {
            return elementCollection;
        }

        @Override
        public ConfigElement get(int index) {
            return maybeWrap(list.get(index));
        }

        @Override
        public int size() {
            return list.size();
        }
    }

    private static class LazyMapContainer extends AbstractMap<String, ConfigElement> implements ConfigNode {
        private final Map<?, ?> map;
        private final Collection<ConfigEntry> entryCollection;
        private final Collection<ConfigElement> elementCollection;

        private LazyMapContainer(Map<?, ?> map) {
            this.map = map;
            this.entryCollection = new AbstractCollection<>() {
                @Override
                public Iterator<ConfigEntry> iterator() {
                    Iterator<? extends Map.Entry<?, ?>> backing = map.entrySet().iterator();

                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return backing.hasNext();
                        }

                        @Override
                        public ConfigEntry next() {
                            return convertEntry(backing.next());
                        }
                    };
                }

                @Override
                public int size() {
                    return map.size();
                }
            };
            this.elementCollection = new AbstractCollection<>() {
                @Override
                public Iterator<ConfigElement> iterator() {
                    Iterator<? extends Map.Entry<?, ?>> backing = map.entrySet().iterator();
                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return backing.hasNext();
                        }

                        @Override
                        public ConfigElement next() {
                            Map.Entry<?, ?> next = backing.next();
                            Object key = next.getKey();
                            Object value = next.getValue();

                            if (!(key instanceof String)) {
                                return null;
                            }

                            return maybeWrap(value);
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
        public @UnmodifiableView @NotNull Collection<ConfigEntry> entryCollection() {
            return entryCollection;
        }

        @Override
        public @UnmodifiableView @NotNull Collection<ConfigElement> elementCollection() {
            return elementCollection;
        }

        @Override
        public ConfigElement get(Object key) {
            return maybeWrap(map.get(key));
        }

        @NotNull
        @Override
        public Set<Entry<String, ConfigElement>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public Iterator<Entry<String, ConfigElement>> iterator() {
                    Iterator<? extends Map.Entry<?, ?>> backing = map.entrySet().iterator();
                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return backing.hasNext();
                        }

                        @Override
                        public Entry<String, ConfigElement> next() {
                            return convertEntry(backing.next());
                        }
                    };
                }

                @Override
                public int size() {
                    return map.size();
                }
            };
        }
    }

    private record EqualsEntry(ConfigContainer first, ConfigContainer second, EqualsEntry parent) {
        private EqualsEntry findParent(ConfigContainer container) {
            EqualsEntry entry = this;
            while (entry != null) {
                if (entry.first == container) {
                    return entry;
                }

                entry = entry.parent;
            }

            return null;
        }
    }

    private static final class HashEntry {
        private final ConfigContainer entry;
        private final boolean list;
        private final Iterator<ConfigEntry> entryIterator;

        private final HashEntry parent;
        private final ConfigEntry parentEntry;

        private int hash;

        private HashEntry(ConfigContainer entry, HashEntry parent, ConfigEntry parentEntry) {
            this.entry = entry;
            this.list = entry.isList();
            this.entryIterator = entry.entryCollection().iterator();

            this.parent = parent;
            this.parentEntry = parentEntry;

            this.hash = list ? 1 : 0;
        }

        private void hash(ConfigEntry entry) {
            if (list) {
                hash = hash * 31 + entry.getValue().hashCode();
                return;
            }

            hash = hash + (entry.getKey().hashCode() ^ entry.getValue().hashCode());
        }

        private HashEntry findParent(ConfigContainer parentContainer) {
            HashEntry current = this;
            while (current != null) {
                if (current.entry == parentContainer) {
                    return current;
                }

                current = current.parent;
            }

            return null;
        }
    }

    /**
     * Deeply computes the hashcode of a {@link ConfigElement}. Can be safely used on elements that contain reference
     * cycles. Additionally, for {@link ConfigList} and {@link ConfigNode} implementations, the result will conform to
     * the {@link AbstractList#hashCode()} and {@link AbstractMap#hashCode()} specification, respectively.
     *
     * @param element the element to compute the hashcode of
     * @return the hashcode
     */
    public static int hashCode(@Nullable ConfigElement element) {
        if (element == null) {
            return 0;
        }

        if (!element.isContainer()) {
            return element.hashCode();
        }

        ConfigContainer rootContainer = element.asContainer();
        if (rootContainer.entryCollection().isEmpty()) {
            return rootContainer.isList() ? 1 : 0;
        }

        Deque<HashEntry> stack = new ArrayDeque<>();

        HashEntry root = new HashEntry(rootContainer, null, null);
        stack.push(root);

        //depth-first traversal
        while (!stack.isEmpty()) {
            HashEntry current = stack.peek();

            if (!current.entryIterator.hasNext()) {
                stack.pop();

                HashEntry parent = current.parent;
                if (parent != null) {
                    parent.hash(current.parentEntry);
                }

                continue;
            }

            while (current.entryIterator.hasNext()) {
                ConfigEntry entry = current.entryIterator.next();
                ConfigElement entryElement = entry.getValue();

                if (entryElement.isScalar()) {
                    current.hash(entry);
                    continue;
                }

                ConfigContainer container = entryElement.asContainer();

                HashEntry parent = current.findParent(container);
                if (parent != null) {
                    continue;
                }

                stack.push(new HashEntry(entryElement.asContainer(), current, entry));
                break;
            }
        }

        return root.hash;
    }

    static ElementType typeFromObject(Object object) {
        if(object instanceof ConfigElement element) {
            return element.type();
        }

        if (object instanceof Map<?,?>) {
            return ElementType.NODE;
        }

        if(object instanceof List<?>) {
            return ElementType.LIST;
        }

        return null;
    }

    private static ConfigEntry convertEntry(Map.Entry<?, ?> entry) {
        Object key = entry.getKey();
        Object value = entry.getValue();

        if (!(key instanceof String string)) {
            return null;
        }

        ConfigElement element = maybeWrap(value);
        if (element == null) {
            return null;
        }

        return ConfigEntry.of(string, element);
    }

    private static ConfigElement maybeWrap(Object o) {
        if (o instanceof ConfigElement element) {
            return element;
        }

        if (o instanceof List<?> || o instanceof Map<?,?>) {
            return containerWrapper(o);
        }

        return null;
    }

    private static ConfigContainer containerWrapper(Object listOrMap) {
        if (listOrMap instanceof ConfigContainer container) {
            return container;
        }

        if (listOrMap instanceof List<?> list) {
            return new LazyListContainer(list);
        }
        else if (listOrMap instanceof Map<?,?> map) {
            return new LazyMapContainer(map);
        }

        throw new IllegalStateException("unexpected object type");
    }

    /**
     * Determines if a given ConfigElement is equal to another object. As per the respective specifications of
     * {@link Map#equals(Object)} and {@link List#equals(Object)}, {@link ConfigNode} and {@link ConfigList} objects can
     * be equal to other implementations of Map and List.
     *
     * @param first the ConfigElement
     * @param second object to be compared to
     * @return true iff the objects are equal; false otherwise
     */
    public static boolean equals(@Nullable ConfigElement first, @Nullable Object second) {
        if (first == second) {
            return true;
        }

        if (first == null ^ second == null) {
            return false;
        }

        if (first.type() != typeFromObject(second)) {
            return false;
        }

        if (!first.isContainer()) {
            //for non-containers, delegate to their equals() method
            return first.equals(second);
        }

        ConfigContainer secondContainer = containerWrapper(second);
        if (first.asContainer().entryCollection().isEmpty()) {
            return secondContainer.entryCollection().isEmpty();
        }

        Deque<EqualsEntry> stack = new ArrayDeque<>();
        stack.push(new EqualsEntry(first.asContainer(), secondContainer, null));

        while (!stack.isEmpty()) {
            EqualsEntry current = stack.pop();
            Collection<ConfigEntry> firstEntries = current.first.entryCollection();
            Collection<ConfigEntry> secondEntries = current.second.entryCollection();

            if (firstEntries.size() != secondEntries.size()) {
                return false;
            }

            boolean significantOrder = current.first.isList();

            Iterator<ConfigEntry> firstIterator = firstEntries.iterator();
            Iterator<ConfigEntry> secondIterator = significantOrder ? secondEntries.iterator() : null;

            while (true) {
                boolean firstHasNext = firstIterator.hasNext();

                ConfigElement firstValue;
                ConfigElement secondValue;

                if (significantOrder) {
                    boolean secondHasNext = secondIterator.hasNext();
                    if (firstHasNext ^ secondHasNext) {
                        //unequal lengths after all (multithreading? not our concern but return false anyway)
                        return false;
                    }

                    //neither have next, don't continue
                    if (!firstHasNext) {
                        break;
                    }

                    ConfigEntry firstNext = firstIterator.next();
                    ConfigEntry secondNext = secondIterator.next();
                    if (secondNext == null) {
                        //incompatible type!
                        return false;
                    }

                    firstValue = firstNext.getValue();
                    secondValue = secondNext.getValue();
                }
                else {
                    if (!firstHasNext) {
                        break;
                    }

                    ConfigEntry firstNext = firstIterator.next();
                    String key = firstNext.getKey();

                    ConfigElement compare = current.second.asNode().get(key);
                    if (compare == null) {
                        return false;
                    }

                    firstValue = firstNext.getValue();
                    secondValue = compare;
                }

                EquateResult result = shallowEquate(firstValue, secondValue);
                if (result == EquateResult.NOT_EQUALS) {
                    //fast exit
                    return false;
                }

                if (result == EquateResult.EQUALS) {
                    continue;
                }

                ConfigContainer firstContainer = firstValue.asContainer();

                EqualsEntry parent = current.findParent(firstContainer);
                if (parent != null) {
                    if (parent.second != secondValue) {
                        return false;
                    }

                    continue;
                }

                stack.push(new EqualsEntry(firstContainer, secondValue.asContainer(), current));
            }
        }

        return true;
    }

    private enum EquateResult {
        EQUALS,
        NOT_EQUALS,
        UNSURE
    }

    private static EquateResult shallowEquate(ConfigElement first, ConfigElement second) {
        if (first == second) {
            return EquateResult.EQUALS;
        }

        if (first.type() != second.type()) {
            return EquateResult.NOT_EQUALS;
        }

        if (!first.isContainer()) {
            return first.equals(second) ? EquateResult.EQUALS : EquateResult.NOT_EQUALS;
        }

        Collection<ConfigEntry> firstCollection = first.asContainer().entryCollection();
        Collection<ConfigEntry> secondCollection = second.asContainer().entryCollection();
        if (firstCollection.isEmpty() && secondCollection.isEmpty()) {
            return EquateResult.EQUALS;
        }

        return firstCollection.size() == secondCollection.size() ? EquateResult.UNSURE : EquateResult.NOT_EQUALS;
    }

    private static class ToStringEntry {
        private final StringBuilder builder;
        private final ToStringEntry parent;
        private final ConfigContainer container;
        private final Iterator<ConfigEntry> containerIterator;
        private final boolean list;
        private final String parentKey;
        private final int absoluteStartIndex;
        private int count;
        private int reference;
        private boolean added;

        private ToStringEntry(ToStringEntry parent, ConfigContainer container, String parentKey, int absoluteStartIndex) {
            this.builder = new StringBuilder(initBuilder(container));
            this.parent = parent;
            this.container = container;
            this.containerIterator = container.entryCollection().iterator();
            this.list = container.isList();
            this.parentKey = parentKey;
            this.absoluteStartIndex = absoluteStartIndex;
            reference = -1;
        }

        private ToStringEntry findParent(ConfigContainer container) {
            ToStringEntry entry = this;
            while (entry != null) {
                if (entry.container == container) {
                    return entry;
                }

                entry = entry.parent;
            }

            return null;
        }

        private static String initBuilder(ConfigContainer container) {
            boolean list = container.isList();

            Collection<ConfigEntry> entries = container.entryCollection();
            if (entries.isEmpty()) {
                return list ? "[]" : "{}";
            }

            return list ? "[" : "{";
        }

        private void append(String key, CharSequence sequence) {
            if (key == null) {
                builder.append(sequence);
            }
            else {
                builder.append(key);
                builder.append("=");
                builder.append(sequence);
            }

            if (count < container.entryCollection().size() - 1) {
                builder.append(", ");
            }
            else {
                builder.append(list ? "]" : "}");
            }

            count++;
        }
    }

    private static final String TAG_PREFIX = "&";

    /**
     * Alternative to {@link ConfigElements#toString(ConfigElement)} designed for outputting to a generic {@link Writer}.
     * Does not close {@code output}.
     * @param input the input ConfigElement
     * @param output the output writer
     * @throws IOException if an IOException occurs when writing to the writer
     */
    public static void toString(@NotNull ConfigElement input, @NotNull Writer output) throws IOException {
        if (input.isNull()) {
            output.write("null");
            return;
        }

        if (!input.isContainer()) {
            output.write(input.toString());
            return;
        }

        ConfigContainer rootContainer = input.asContainer();
        if (rootContainer.entryCollection().isEmpty()) {
            output.write(rootContainer.isList() ? "[]" : "{}");
            return;
        }

        Deque<ToStringEntry> stack = new ArrayDeque<>();
        ToStringEntry root = new ToStringEntry(null, rootContainer, null, 0);
        stack.push(root);

        int referenced = 0;
        StringBuilder tagBuffer = new StringBuilder(2);
        tagBuffer.append(TAG_PREFIX);

        while (!stack.isEmpty()) {
            ToStringEntry current = stack.peek();
            if (!current.containerIterator.hasNext()) {
                stack.pop();

                ToStringEntry parent = current.parent;
                if (parent != null) {
                    parent.append(current.parentKey, current.builder);
                    current.added = true;
                }
                continue;
            }

            while (current.containerIterator.hasNext()) {
                ConfigEntry next = current.containerIterator.next();
                ConfigElement nextElement = next.getValue();

                if (!nextElement.isContainer()) {
                    current.append(next.getKey(), nextElement.toString());
                    continue;
                }

                ConfigContainer nextContainer = nextElement.asContainer();
                ToStringEntry entry = current.findParent(nextContainer);
                if (entry != null) {
                    if (entry.reference == -1) {
                        entry.reference = referenced++;

                        tagBuffer.append(entry.reference);

                        if (entry.added) {
                            root.builder.insert(entry.absoluteStartIndex, tagBuffer);
                        }
                        else {
                            entry.builder.insert(0, tagBuffer);
                        }

                        current.append(next.getKey(), tagBuffer);
                    }
                    else {
                        tagBuffer.append(entry.reference);
                        current.append(next.getKey(), tagBuffer);
                    }

                    tagBuffer.setLength(1);
                    continue;
                }

                String nextKey = next.getKey();
                int rootLength = root.builder.length();
                entry = new ToStringEntry(current, nextContainer, current.list ? null : nextKey,
                    nextKey == null ? rootLength : rootLength + nextKey.length() + 1);
                stack.push(entry);
                break;
            }
        }

        output.append(root.builder);
    }

    /**
     * <p>Specialized helper method used by {@link ConfigContainer} implementations that need to override
     * {@link Object#toString()}. Supports circular and self-referential ConfigElement constructions by use of a "tag"
     * syntax: containers are associated with a <i>name</i>, and if a reference cycle occurs, the reference will be
     * shown by use of the tag.</p>
     * <p>
     * Strings returned by this method are also valid Propylene configuration, which can be passed to
     * {@link ConfigElement#of(String)} in order to reconstruct the object graph from its string representation.
     * <p>
     * {@code input} will not be closed by this method.
     *
     * @param input the input {@link ConfigElement} to show
     * @return the ConfigElement, represented as a string
     */
    public static @NotNull String toString(@NotNull ConfigElement input) {
        try (StringWriter writer = new StringWriter()) {
            toString(input, writer);
            return writer.toString();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
