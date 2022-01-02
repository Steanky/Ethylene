package com.github.steank.ethylene.collection;

import com.github.steank.ethylene.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.*;
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
     * The backing map for this AbstractConfigNode
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
    public @NotNull Optional<ConfigElement> getElement(@NotNull String... keys) {
        Objects.requireNonNull(keys);
        if(keys.length == 0) {
            throw new IllegalArgumentException("keys was empty");
        }
        else if(keys.length == 1) { //simplest case, just return directly from our map
            return Optional.ofNullable(mappings.get(keys[0]));
        }
        else { //iterate through the provided keys
            ConfigNode current = this;
            int lastIndex = keys.length - 1;
            for(int i = 0; i < keys.length; i++) {
                Optional<ConfigElement> childOptional = current.getElement(keys[i]);

                if(i == lastIndex) { //we got to the last key, so return whatever we find
                    return childOptional;
                }
                else if(childOptional.isPresent()) {
                    ConfigElement childElement = childOptional.get();
                    if(childElement.getType().isNode()) { //continue traversing nodes...
                        current = childElement.asConfigNode();
                    }
                    else { //if we still have nodes to traverse, but ran into something that's NOT a node, return
                        return Optional.empty();
                    }
                }
                else { //there is no element here, return
                    return Optional.empty();
                }
            }
        }

        return Optional.empty();
    }

    /**
     * This helper method can be used to construct a map with the same elements as another map. If the given map
     * contains any null keys or values, a {@link NullPointerException} will be thrown.
     * @param map the map whose elements will be added to the returned map
     * @param mapSupplier The supplier used to create the map
     * @param <T> the type of the map to construct
     * @return a new map, constructed by the supplier, and containing the same elements as map
     * @throws NullPointerException if map contains any null keys or values
     */
    protected static <T extends Map<String, ConfigElement>> T constructMap(@NotNull Map<String, ConfigElement> map,
                                                                           @NotNull Supplier<T> mapSupplier) {
        Objects.requireNonNull(map);
        Objects.requireNonNull(mapSupplier);

        T newMap = mapSupplier.get();
        for(Map.Entry<String, ConfigElement> entry : map.entrySet()) {
            newMap.put(Objects.requireNonNull(entry.getKey(), "input map must not contain null keys"),
                    Objects.requireNonNull(entry.getValue(), "input map must not contain null values"));
        }

        return newMap;
    }
}
