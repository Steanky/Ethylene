package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An implementation of {@link ConfigNode} based off of {@link HashMap}. It is provided because it can have better
 * memory usage and performance characteristics than {@link LinkedConfigNode}, but as iteration order will not
 * necessarily reflect insertion order, it is not recommended for general use.
 *
 * @see HashMap
 * @see LinkedConfigNode
 */
public class HashConfigNode extends BackedConfigNode {
    /**
     * Constructs a new HashConfigNode backed by an empty {@link HashMap}.
     */
    public HashConfigNode() {
        super(new HashMap<>());
    }

    /**
     * Constructs a new HashConfigNode with the same entries as the provided map, backed by a {@link HashMap}.
     *
     * @param mappings the mappings to initialize this object with
     * @throws NullPointerException if mappings is null or contains any null keys or values
     */
    public HashConfigNode(@NotNull Map<? extends String, ? extends ConfigElement> mappings) {
        super(constructMap(mappings, HashMap::new));
    }

    /**
     * Constructs a new HashConfigNode backed by an empty {@link HashMap} with the given initial capacity.
     *
     * @param initialCapacity the initial capacity
     */
    public HashConfigNode(int initialCapacity) {
        super(new HashMap<>(initialCapacity));
    }

    /**
     * Constructs a new HashConfigNode backed by an empty {@link HashMap} with the given initial capacity and load
     * factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor      the load factor
     */
    public HashConfigNode(int initialCapacity, float loadFactor) {
        super(new HashMap<>(initialCapacity, loadFactor));
    }

    @Override
    public @NotNull ConfigContainer emptyCopy() {
        return new HashConfigNode(size());
    }
}
