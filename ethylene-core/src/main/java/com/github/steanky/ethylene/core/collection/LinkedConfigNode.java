package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;

/**
 * An implementation of {@link ConfigNode} based off of {@link LinkedHashMap}, sometimes preferred over {@link HashMap}
 * because it maintains insertion order when iterating, and has better iteration performance.
 *
 * @see LinkedHashMap
 * @see HashConfigNode
 */
public class LinkedConfigNode extends BackedConfigNode {
    /**
     * Constructs a new LinkedConfigNode backed by an empty {@link LinkedHashMap}.
     */
    public LinkedConfigNode() {
        super(new LinkedHashMap<>());
    }

    /**
     * Constructs a new LinkedConfigNode with the same entries as the provided map, backed by a {@link LinkedHashMap}.
     *
     * @param mappings the mappings to initialize this object with
     * @throws NullPointerException if mappings is null or contains any null keys or values
     */
    public LinkedConfigNode(@NotNull Map<? extends String, ? extends ConfigElement> mappings) {
        super(constructMap(mappings, LinkedHashMap::new));
    }

    /**
     * Constructs a new HashConfigNode backed by an empty {@link LinkedHashMap} with the given initial capacity.
     *
     * @param initialCapacity the initial capacity
     */
    public LinkedConfigNode(int initialCapacity) {
        super(new LinkedHashMap<>(initialCapacity));
    }

    /**
     * Constructs a new HashConfigNode backed by an empty {@link LinkedHashMap} with the given initial capacity and
     * load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor      the load factor
     */
    public LinkedConfigNode(int initialCapacity, float loadFactor) {
        super(new LinkedHashMap<>(initialCapacity, loadFactor));
    }

    /**
     * Constructs a new HashConfigNode backed by an empty {@link LinkedHashMap} with the given initial capacity,load
     * factor, and access order characteristics. Corresponds to
     * {@link LinkedHashMap#LinkedHashMap(int, float, boolean)}.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor      the load factor
     * @param accessOrder     true for access-order, false for insertion-order
     */
    public LinkedConfigNode(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(new LinkedHashMap<>(initialCapacity, loadFactor, accessOrder));
    }

    @Override
    public @NotNull ConfigContainer emptyCopy() {
        return new LinkedConfigNode(size());
    }
}
