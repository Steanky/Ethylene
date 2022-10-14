package com.github.steanky.ethylene.core.collection;

/**
 * Marker interface indicating a collection or map is "immutable"; i.e. mutating methods, such as
 * {@link java.util.Collection#add(Object)}, or {@link java.util.Map#put(Object, Object)}, will throw an exception if
 * called, <b>and</b> the reported contents of the map or collection will not change throughout its usable lifetime.
 * This is in contrast to {@link ImmutableView}, whose contents <i>can</i> change if the underlying map or collection
 * is mutated.
 * @see ImmutableView
 */
public interface Immutable {
}
