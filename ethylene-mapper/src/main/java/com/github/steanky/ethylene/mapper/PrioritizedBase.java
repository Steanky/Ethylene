package com.github.steanky.ethylene.mapper;

import org.jetbrains.annotations.NotNull;

/**
 * Base implementation of {@link Prioritized}. Implements {@link Comparable#compareTo(Object)} and
 * {@link Prioritized#priority()}. This is mainly intended for subclassing, but can be instantiated on its own.
 * <p>
 * This object's priority, and the priority of subclasses, will not change over its lifespan.
 */
public class PrioritizedBase implements Prioritized {
    private final int priority;

    /**
     * Creates a new instance of this class with the specified priority.
     *
     * @param priority the priority of this object
     */
    public PrioritizedBase(int priority) {
        this.priority = priority;
    }

    @Override
    public final int compareTo(@NotNull Prioritized o) {
        return Integer.compare(o.priority(), priority);
    }

    @Override
    public final int priority() {
        return priority;
    }
}
