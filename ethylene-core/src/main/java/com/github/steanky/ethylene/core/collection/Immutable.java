package com.github.steanky.ethylene.core.collection;

/**
 * Marker interface indicating a collection or map is "immutable"; i.e. mutating methods, such as
 * {@link java.util.Collection#add(Object)}, or {@link java.util.Map#put(Object, Object)}, will throw an exception if
 * called.
 */
public interface Immutable {
}
