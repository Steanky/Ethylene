package com.github.steanky.ethylene.core.collection;

/**
 * Marker interface used to indicate that the {@link ConfigContainer} implementation does not support direct
 * modification. However, it <i>may</i> be modified if a backing container is changed.
 * <p>
 * Implementations must ensure that any sub-containers are also immutable in this way.
 * @see Immutable
 */
public interface ImmutableView {
}
