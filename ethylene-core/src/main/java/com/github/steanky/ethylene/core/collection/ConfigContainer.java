package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.util.ConfigElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.Map;

/**
 * Represents a {@link ConfigElement} capable of holding other ConfigElements. ConfigContainer objects are generally
 * also either {@link Map} or {@link Collection} implementations.
 */
public interface ConfigContainer extends ConfigElement {
    @Override
    default @NotNull ConfigContainer asContainer() {
        return this;
    }

    @Override
    default boolean isContainer() {
        return true;
    }

    /**
     * Returns the <i>entry collection</i> maintained by this ConfigContainer. The collection must be immutable and
     * read-through, so changes in the underlying container are reflected in the entry collection. Additionally,
     * repeated calls to this method should return the same instance.
     *
     * @return an immutable, read-through collection representing the entries contained in this object
     */
    @UnmodifiableView @NotNull Collection<ConfigEntry> entryCollection();

    /**
     * Returns the <i>element collection</i> maintained by this ConfigContainer. The collection must be immutable and
     * read-through, so changes in the underlying container are reflected in the collection. Additionally, repeated
     * calls to this method should return the same instance.
     *
     * @return an immutable, read-through collection representing the {@link ConfigElement}s contained in this object
     */
    @UnmodifiableView @NotNull Collection<ConfigElement> elementCollection();

    /**
     * Creates a new, empty {@link ConfigContainer} implementation with the same capacity and characteristics as this
     * one. Implementations may choose not to implement this method. If it is supported, the returned container must
     * be of the same class as the container in which it is contained.
     * <p>
     * Implementations that do not implement this method will not be correctly copied using
     * {@link ConfigContainer#copy()}; they will be replaced by a default type instead.
     *
     * @return a new, empty ConfigContainer
     * @throws UnsupportedOperationException if this ConfigContainer implementation does not support this functionality
     */
    default @NotNull ConfigContainer emptyCopy() {
        throw new UnsupportedOperationException("This ConfigContainer does not support copying");
    }

    /**
     * Creates an exact, deep copy of this {@link ConfigContainer}, preserving the entire configuration tree, including
     * circular references. Each container present in the new tree is guaranteed to be a different object than its
     * equivalent in the original tree. However, it is unspecified whether scalars are copied or not; the original
     * instances may or may not be reused, dependent on the implementation.
     *
     * @return an exact, deep copy of this ConfigContainer
     */
    default @NotNull ConfigContainer copy() {
        return ConfigElementUtils.clone(this);
    }
}
