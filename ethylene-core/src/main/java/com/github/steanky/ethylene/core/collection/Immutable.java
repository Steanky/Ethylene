package com.github.steanky.ethylene.core.collection;

/**
 * Marker interface indicating a {@link ConfigContainer} implementation is immutable; i.e. its mutating methods, like
 * {@link ConfigList#add(Object)} or {@link ConfigNode#put(Object, Object)}, will throw an exception if called, and
 * cannot change the container itself. However, the contents may still change if there is an underlying container
 * that is mutable.
 * <p>
 * Additionally, implementations must ensure that any sub-containers are also immutable.
 */
public interface Immutable {
}
