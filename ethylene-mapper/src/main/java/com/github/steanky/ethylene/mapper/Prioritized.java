package com.github.steanky.ethylene.mapper;

/**
 * Represents something that has a priority, and can be ordered relative to other {@link Prioritized} objects.
 * <p>
 * Prioritized implementations are generally expected to have natural orderings that are <i>not</i> consistent with
 * {@link Object#equals(Object)}. An object's priority is thus only to be used for ordering within a list or array, and
 * not for navigable map implementations where order and identity are intrinsically linked.
 */
public interface Prioritized extends Comparable<Prioritized> {
    /**
     * The priority of this object. Can be any integer, which is allowed to change over the object's lifetime.
     *
     * @return this object's priority
     */
    int priority();
}
