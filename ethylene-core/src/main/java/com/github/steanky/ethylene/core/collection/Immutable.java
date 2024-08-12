package com.github.steanky.ethylene.core.collection;

/**
 * Marker interface indicating a {@link ConfigContainer} implementation is immutable; i.e. its mutating methods, like
 * {@link ConfigList#add(Object)} or {@link ConfigNode#put(Object, Object)}, will throw an exception if called, and
 * cannot change the container itself. Similarly, the reported contents of the container may <i>not</i> change over its
 * lifetime.
 * <p>
 * Implementations must ensure that any sub-containers are also immutable in this way.
 * @see ImmutableView
 */
public interface Immutable extends ImmutableView {
}
