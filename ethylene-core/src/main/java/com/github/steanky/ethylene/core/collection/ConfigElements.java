package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;

/**
 * Simple utilities for ConfigElements.
 */
final class ConfigElements {
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

    private record StackEntry(ConfigContainer first, ConfigContainer second) {

    }

    private static final class HashEntry {
        private final ConfigContainer entry;
        private final boolean list;
        private final Iterator<ConfigEntry> entryIterator;

        private final HashEntry parent;
        private final String parentKey;

        private int hash;

        private HashEntry(ConfigContainer entry, HashEntry parent, String parentKey) {
            this.entry = entry;
            this.list = entry.isList();
            this.entryIterator = entry.entryCollection().iterator();

            this.parent = parent;
            this.parentKey = parentKey;

            this.hash = list ? 1 : 0;
        }

        private void hash(int code) {
            hash = list ? (hash * 31 + code) : hash + code;
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
    static int hashCode(@Nullable ConfigElement element) {
        if (element == null) {
            return 0;
        }

        if (element.isScalar()) {
            return element.hashCode();
        }

        ConfigContainer first = element.asContainer();
        Deque<HashEntry> stack = new ArrayDeque<>();

        HashEntry root = new HashEntry(first, null, null);
        stack.push(root);

        Set<ConfigContainer> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        visited.add(first);

        //depth-first traversal
        while (!stack.isEmpty()) {
            HashEntry hashEntry = stack.peek();

            if (!hashEntry.entryIterator.hasNext()) {
                stack.pop();

                HashEntry parent = hashEntry.parent;
                if (parent != null) {
                    parent.hash(hashEntry.parentKey == null ? hashEntry.hash :
                        (hashEntry.parentKey.hashCode() ^ hashEntry.hash));
                }

                continue;
            }

            while (hashEntry.entryIterator.hasNext()) {
                ConfigEntry entry = hashEntry.entryIterator.next();
                ConfigElement entryElement = entry.getValue();

                if (entryElement.isScalar()) {
                    hashEntry.hash(hashEntry.list ? entryElement.hashCode() :
                        (entry.getKey().hashCode() ^ entryElement.hashCode()));
                    continue;
                }

                ConfigContainer container = entryElement.asContainer();
                if (!visited.add(container)) {
                    continue;
                }

                stack.push(new HashEntry(entryElement.asContainer(), hashEntry,
                    hashEntry.entry.isNode() ? entry.getKey() : null));
                break;
            }
        }

        return root.hash;
    }

    static ElementType type(Object object) {
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

    static boolean equals(@Nullable ConfigElement first, @Nullable Object second) {
        if (first == second) {
            return true;
        }

        if (first == null ^ second == null) {
            return false;
        }

        if (first.type() != type(second)) {
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

        Deque<StackEntry> stack = new ArrayDeque<>();
        stack.push(new StackEntry(first.asContainer(), secondContainer));

        //visited containers
        Set<ConfigContainer> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        visited.add(first.asContainer());

        while (!stack.isEmpty()) {
            StackEntry current = stack.pop();
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

                //cycle detected!
                //we already visited this container, meaning we already compared it, so do nothing
                if (visited.contains(firstContainer)) {
                    continue;
                }

                stack.push(new StackEntry(firstContainer, secondValue.asContainer()));
                visited.add(firstContainer);
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

        return first.asContainer().entryCollection().size() == second.asContainer().entryCollection().size() ?
            EquateResult.UNSURE : EquateResult.NOT_EQUALS;
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

        private static String initBuilder(ConfigContainer container) {
            boolean list = container.isList();

            Collection<ConfigEntry> entries = container.entryCollection();
            if (entries.isEmpty()) {
                return list ? "[]" : "{}";
            }

            return list ? "[" : "{";
        }

        private void append(String key, String string) {
            String formatted = format(key, string);

            if (count == 0) {
                builder.append(formatted);
            }
            else if (count < container.entryCollection().size()) {
                builder.append(", ");
                builder.append(formatted);
            }

            if (count == container.entryCollection().size() - 1) {
                builder.append(list ? "]" : "}");
            }

            count++;
        }

        private static String format(String key, String value) {
            return key == null ? value : key + "=" + value;
        }
    }

    /**
     * <p>Specialized helper method used by {@link ConfigContainer} implementations that need to override
     * {@link Object#toString()}. Supports circular and self-referential ConfigElement constructions by use of a "tag"
     * syntax: containers are associated with a <i>name</i>, and if a container occurs twice, it will be referred to by
     * the name the second time rather than showing its entire contents again.</p>
     *
     * @param input the input {@link ConfigElement} to show
     * @return the ConfigElement, represented as a string
     */
    static @NotNull String toString(ConfigElement input) {
        if (input == null) {
            return "null";
        }

        if (!input.isContainer()) {
            return input.toString();
        }

        ConfigContainer rootContainer = input.asContainer();
        if (rootContainer.entryCollection().isEmpty()) {
            return rootContainer.isList() ? "[]" : "{}";
        }

        Deque<ToStringEntry> stack = new ArrayDeque<>();
        ToStringEntry root = new ToStringEntry(null, rootContainer, null, 0);
        stack.push(root);

        Map<ConfigContainer, ToStringEntry> visited = new IdentityHashMap<>();
        visited.put(rootContainer, root);

        int referenced = 0;
        int offset = 0;
        while (!stack.isEmpty()) {
            ToStringEntry current = stack.peek();
            if (!current.containerIterator.hasNext()) {
                stack.pop();

                ToStringEntry parent = current.parent;
                if (parent != null) {
                    parent.append(current.parentKey, current.builder.toString());
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
                ToStringEntry entry = visited.get(nextContainer);
                if (entry != null) {
                    if (entry.reference == -1) {
                        entry.reference = referenced++;
                        String tag = "$" + entry.reference;

                        if (entry.added) {
                            root.builder.insert(entry.absoluteStartIndex + offset, tag);
                        }
                        else {
                            entry.builder.insert(0, tag);
                        }

                        current.append(next.getKey(), tag);
                        offset += tag.length();
                    }
                    else {
                        current.append(next.getKey(), "$" + entry.reference);
                    }

                    continue;
                }

                String nextKey = next.getKey();
                int rootLength = root.builder.length();
                entry = new ToStringEntry(current, nextContainer, current.list ? null : nextKey,
                    nextKey == null ? rootLength : rootLength + nextKey.length() + 1);
                visited.put(nextContainer, entry);
                stack.push(entry);
                break;
            }
        }

        return root.builder.toString();
    }
}
