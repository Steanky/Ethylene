package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.util.ConfigElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * <p>Contains functionality and methods common to {@link ConfigNode} implementations. This abstract class does not
 * define any abstract methods. Its main use is to enable concrete implementations to specify what kind of backing map
 * should be used.</p>
 *
 * <p>Subclasses must take care to ensure that the map used to construct this object does not contain null keys or
 * values at any point. Therefore, the backing map should not be exposed anywhere where it may be accidentally used. See
 * {@link LinkedConfigNode} for an example of how to properly inherit this class.</p>
 */
public abstract class AbstractConfigNode extends AbstractMap<String, ConfigElement> implements ConfigNode {
    /**
     * The backing map for this AbstractConfigNode. Implementations that access this variable must take care to ensure
     * that null values cannot be inserted, and the map is never exposed publicly.
     */
    protected final Map<String, ConfigElement> mappings;
    private Collection<ConfigEntry> containerCollection;

    /**
     * Construct a new AbstractConfigNode using the provided mappings.
     * @param mappings the mappings to use
     * @throws NullPointerException if mappings is null
     */
    protected AbstractConfigNode(@NotNull Map<String, ConfigElement> mappings) {
        this.mappings = Objects.requireNonNull(mappings);
    }

    @Override
    public ConfigElement get(Object key) {
        return mappings.get(Objects.requireNonNull(key));
    }

    @Override
    public ConfigElement put(@NotNull String key, @NotNull ConfigElement value) {
        return mappings.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
    }

    @Override
    public boolean containsKey(Object key) {
        return mappings.containsKey(Objects.requireNonNull(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return mappings.containsValue(Objects.requireNonNull(value));
    }

    @Override
    public ConfigElement remove(Object key) {
        return mappings.remove(Objects.requireNonNull(key));
    }

    @Override
    public void clear() {
        mappings.clear();
    }

    @NotNull
    @Override
    public Set<Entry<String, ConfigElement>> entrySet() {
        return mappings.entrySet();
    }

    @Override
    public @NotNull Collection<ConfigEntry> entryCollection() {
        if(containerCollection != null) {
            return containerCollection;
        }

        return containerCollection = new AbstractCollection<>() {
            @Override
            public Iterator<ConfigEntry> iterator() {
                return new Iterator<>() {
                    private final Iterator<Map.Entry<String, ConfigElement>> entryIterator = mappings.entrySet()
                            .iterator();

                    @Override
                    public boolean hasNext() {
                        return entryIterator.hasNext();
                    }

                    @Override
                    public ConfigEntry next() {
                        Entry<String, ConfigElement> next = entryIterator.next();
                        return new ConfigEntry(next.getKey(), next.getValue());
                    }
                };
            }

            @Override
            public int size() {
                return mappings.size();
            }
        };
    }

    @Override
    public String toString() {
        return ConfigElementUtils.toString(this);
    }

    /**
     * This helper method can be used to construct a map with the same elements as another map. If the given map
     * contains any null keys or values, a {@link NullPointerException} will be thrown. Furthermore, each value will
     * be tested against the provided {@link Predicate}. If it returns false, an {@link IllegalArgumentException} will
     * be thrown.
     * @param map the map whose elements will be added to the returned map
     * @param mapSupplier the supplier used to create the new map from the size of the original map
     * @param valuePredicate the predicate to use to validate each element against some condition
     * @return a new map, constructed by the supplier, and containing the same elements as map
     * @throws NullPointerException if any of the arguments are null, or map contains any null keys or values
     * @throws IllegalArgumentException if the given predicate fails for any of the map's values
     */
    protected static @NotNull Map<String, ConfigElement> constructMap(
            @NotNull Map<? extends String, ? extends ConfigElement> map,
            @NotNull IntFunction<? extends Map<String, ConfigElement>> mapSupplier,
            @NotNull Predicate<ConfigElement> valuePredicate) {
        Objects.requireNonNull(map);
        Objects.requireNonNull(mapSupplier);
        Objects.requireNonNull(valuePredicate);

        Map<String, ConfigElement> newMap = mapSupplier.apply(map.size());
        for(Map.Entry<? extends String, ? extends ConfigElement> entry : map.entrySet()) {
            if(!valuePredicate.test(entry.getValue())) {
                throw new IllegalArgumentException("Value predicate failed");
            }
            else {
                newMap.put(Objects.requireNonNull(entry.getKey(), "Input map must not contain null keys"),
                        Objects.requireNonNull(entry.getValue(), "Input map must not contain null values"));
            }
        }

        return newMap;
    }
}
