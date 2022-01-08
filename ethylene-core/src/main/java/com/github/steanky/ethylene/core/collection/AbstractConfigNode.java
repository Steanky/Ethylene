package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * <p>Contains functionality and methods common to {@link ConfigNode} implementations. This abstract class does not
 * define any abstract methods. Its main use is to enable concrete implementations to specify what kind of backing map
 * should be used.</p>
 *
 * <p>Subclasses must take care to ensure that the map used to construct this object does not contain null keys or
 * values at any point. Therefore, the backing map should not be exposed anywhere where it may be accidentally used. See
 * {@link LinkedConfigNode} and {@link FileConfigNode} for examples of how to properly inherit this class.</p>
 */
public abstract class AbstractConfigNode extends AbstractMap<String, ConfigElement> implements ConfigNode {
    /**
     * The backing map for this AbstractConfigNode.
     */
    protected final Map<String, ConfigElement> mappings;

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
    public ConfigElement getElement(@NotNull String... keys) {
        Objects.requireNonNull(keys);

        if(keys.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty");
        }
        else if(keys.length == 1) { //simplest case, just return directly from our map
            return mappings.get(Objects.requireNonNull(keys[0]));
        }
        else { //iterate through the provided keys, since length > 1
            ConfigNode current = this;
            ConfigElement value = null;

            for (String key : keys) {
                if(current == null) {
                    return null;
                }
                else {
                    value = current.getElement(key);

                    if (value == null) {
                        //we failed to find something for this key, so return null
                        return null;
                    }
                    else {
                        if (value.isNode()) {
                            //continue traversing nodes...
                            current = value.asNode();
                        } else {
                            //we still have nodes to traverse, but ran into something that's not a node — set current to
                            //null. if we're on the last node, we will return when this loop finishes anyway. if we're
                            //NOT on the last node, we'll return null as we still have keys, indicating an invalid path
                            current = null;
                        }
                    }
                }
            }

            return value;
        }
    }

    /**
     * This helper method can be used to construct a map with the same elements as another map. If the given map
     * contains any null keys or values, a {@link NullPointerException} will be thrown. Furthermore, each value will
     * be tested against the provided {@link Predicate}. If it returns false, an {@link IllegalArgumentException} will
     * be thrown.
     * @param map the map whose elements will be added to the returned map
     * @param mapSupplier the supplier used to create the map
     * @param valuePredicate the predicate to use to validate each element against some condition
     * @param <TMap> the type of the map to construct
     * @return a new map, constructed by the supplier, and containing the same elements as map
     * @throws NullPointerException if any of the arguments are null, or map contains any null keys or values
     * @throws IllegalArgumentException if the given predicate fails for any of the map's values
     */
    protected static <TMap extends Map<String, ConfigElement>> @NotNull TMap constructMap(
            @NotNull Map<String, ConfigElement> map,
            @NotNull Supplier<TMap> mapSupplier,
            @NotNull Predicate<ConfigElement> valuePredicate) {
        Objects.requireNonNull(map);
        Objects.requireNonNull(mapSupplier);
        Objects.requireNonNull(valuePredicate);

        TMap newMap = mapSupplier.get();
        for(Map.Entry<String, ConfigElement> entry : map.entrySet()) {
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
