package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.RandomAccess;

/**
 * An implementation of {@link ConfigList} based off of an {@link ArrayList}, with similar performance and other
 * characteristics. It implements {@link RandomAccess}.
 */
public class ArrayConfigList extends AbstractConfigList implements RandomAccess {
    /**
     * Constructs a new ArrayConfigList backed by an empty {@link ArrayList}.
     */
    public ArrayConfigList() {
        super(new ArrayList<>());
    }

    /**
     * Constructs a new ArrayConfigList backed an {@link ArrayList} containing the same elements as the provided
     * {@link Collection}. This builder uses
     * {@link AbstractConfigList#constructList(Collection, java.util.function.IntFunction)} to validate that the list
     * has no null elements.
     *
     * @param collection the collection to copy elements from
     */
    public ArrayConfigList(@NotNull Collection<? extends ConfigElement> collection) {
        super(constructList(collection, ArrayList::new));
    }

    /**
     * Constructs a new ArrayConfigList backed by an empty {@link ArrayList} with the given initial capacity.
     *
     * @param initialCapacity the initial capacity
     */
    public ArrayConfigList(int initialCapacity) {
        super(new ArrayList<>(initialCapacity));
    }

    /**
     * Calls {@link ArrayList#trimToSize()} on the internal ArrayList.
     */
    public void trimToSize() {
        ((ArrayList<?>) list).trimToSize();
    }

    @Override
    public @NotNull ConfigContainer emptyCopy() {
        return new ArrayConfigList(size());
    }
}