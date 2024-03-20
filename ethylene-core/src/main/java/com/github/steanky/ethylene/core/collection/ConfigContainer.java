package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.Map;

/**
 * Represents a {@link ConfigElement} capable of holding other ConfigElements. ConfigContainer objects are generally
 * also either {@link Map} or {@link Collection} implementations, but this is not required. However,
 * {@link ConfigElement#isList()} || {@link ConfigElement#isNode()} must be true for all implementations.
 * <p>
 * ConfigContainer implementations may be immutable or mutable. Immutable implementations do not support mutating
 * methods like {@link Collection#add(Object)}, but their contents may still change over their lifetime through other
 * means, for example if they have a backing collection and the backing collection changes. Immutable implementations
 * may be identified by checking if this object is an instance of {@link Immutable}.
 */
public interface ConfigContainer extends ConfigElement {
    @Override
    default @NotNull ConfigContainer asContainer() {
        return this;
    }

    /**
     * Returns the <i>entry collection</i> maintained by this ConfigContainer. The collection must be immutable and
     * read-through, so changes in the underlying container are reflected in the entry collection.
     *
     * @return an immutable, read-through collection representing the entries contained in this object
     */
    @UnmodifiableView @NotNull Collection<ConfigEntry> entryCollection();

    /**
     * Returns the <i>element collection</i> maintained by this ConfigContainer. The collection must be immutable and
     * read-through, so changes in the underlying container are reflected in the collection.
     *
     * @return an immutable, read-through collection representing the {@link ConfigElement}s contained in this object
     */
    @UnmodifiableView @NotNull Collection<ConfigElement> elementCollection();

    /**
     * Creates a new, empty {@link ConfigContainer} implementation with the same initial capacity (if supported) and any
     * other applicable characteristics as this one. Implementations may choose not to implement this method. If it is
     * supported, the returned container must be of the same class.
     * <p>
     * Mutable implementations that do not implement this method will not be correctly copied using
     * {@link ConfigContainer#copy()}; they will be replaced by a default type instead. {@link Immutable}
     * implementations need not override this method, as they will not be copied.
     *
     * @return a new, empty ConfigContainer, or {@code null} if this type does not support copying
     */
    default @Nullable ConfigContainer emptyCopy() {
        return null;
    }

    /**
     * Creates an exact, deep copy of this {@link ConfigContainer}, preserving the entire configuration tree, including
     * circular references. Each container present in the new tree is guaranteed to be a different object than its
     * equivalent in the original tree, except when:
     *
     * <ol>
     *     <li>the element is a scalar type, or</li>
     *     <li>the element is an immutable collection type</li>
     * </ol>
     *
     * @return an exact, deep copy of this ConfigContainer
     */
    default @NotNull ConfigContainer copy() {
        return ConfigContainers.copy(this);
    }

    /**
     * Creates an immutable copy of this ConfigContainer, preserving the entire configuration tree, including circular
     * references. Any sub-containers are converted to an immutable equivalent if necessary. As with
     * {@link ConfigContainer#copy()}, only scalar types and immutable collection types are left unchanged between the
     * input and output graph.
     *
     * @return an immutable copy of this ConfigContainer, whose contents will not change even if this container changes
     */
    default @NotNull ConfigContainer immutableCopy() {
        return ConfigContainers.immutableCopy(this);
    }

    /**
     * Creates an immutable view of this ConfigContainer, preserving the entire configuration tree, including circular
     * references. Any sub-containers are converted to an immutable view equivalent if necessary. As with
     * {@link ConfigContainer#copy()}, only scalar types and immutable collection types are left unchanged between the
     * input and output graph.
     *
     * @return an immutable copy of this ConfigContainer, whose contents will change if this container changes, but
     * that cannot itself be directly modified
     */
    default @NotNull ConfigContainer immutableView() {
        return ConfigContainers.immutableView(this);
    }
}
