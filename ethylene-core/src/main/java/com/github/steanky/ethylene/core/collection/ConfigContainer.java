package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * Represents a {@link ConfigElement} capable of holding other ConfigElements. ConfigContainer objects are generally
 * also either {@link Map} or {@link Collection} implementations, but this is not required. However,
 * {@link ConfigElement#isList()} || {@link ConfigElement#isNode()} must be true for all implementations.
 * <p>
 * ConfigContainer implementations may be immutable or mutable. Immutable implementations do not support mutating
 * methods like {@link Collection#add(Object)}, but their contents may still change over their lifetime through other
 * means, for example if they have a backing collection and the backing collection changes.
 * <p>
 * There are two kinds of immutable containers: {@link Immutable} and {@link ImmutableView}. {@code ImmutableView}
 * instances cannot be modified directly through e.g. {@link Map#put(Object, Object)} or {@link List#add(Object)}.
 * However, they might represent a <i>view</i> of another container object, that is itself mutable and may change.
 * Therefore, the objects contained within an {@code ImmutableView} may indirectly change.
 * <p>
 * {@link Immutable} instances likewise cannot be modified; they are not backed by a mutable list. Their contents are
 * guaranteed to never change. {@code Immutable} is a subset of {@code ImmutableView}.
 */
public interface ConfigContainer extends ConfigElement {
    @Override
    default @NotNull ConfigContainer asContainer() {
        return this;
    }

    /**
     * Returns the <i>entry collection</i> maintained by this ConfigContainer. The collection must be immutable and
     * read-through, so changes in the underlying container are reflected in the entry collection.
     * <p>
     * <b>Important note</b>: The returned collection might not actually store distinct {@link ConfigEntry} objects. It
     * is permissible for an implementation to return a collection whose iterator continually mutates and returns the
     * same entry.
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
     * {@link ConfigContainer#copy()}; they will be replaced by a default type instead. Therefore, overriding this is
     * highly recommended in most cases. However, {@link ImmutableView} implementations should never need to override
     * this method.
     *
     * @return a new, empty ConfigContainer, or {@code null} if this type does not support copying
     */
    default @Nullable ConfigContainer emptyCopy() {
        return null;
    }

    /**
     * Creates an exact, deep copy of this {@link ConfigContainer}, preserving the entire configuration tree, including
     * circular references, and attempts to preserve the type of each container. Each container present in the new tree
     * is guaranteed to be a different object than its equivalent in the original tree, except when:
     *
     * <ol>
     *     <li>the element is a scalar type, or</li>
     *     <li>the element is an immutable collection type</li>
     * </ol>
     * <p>
     * This method will use the {@link ConfigContainer#emptyCopy()} method to construct new instances of each mutable
     * container present in the configuration tree. If {@code emptyCopy} returns {@code null}, the default
     * {@link ConfigNode} is {@link LinkedConfigNode}, and the default {@link ConfigList} is {@link ArrayConfigList}.
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
     * Creates a mutable copy of this ConfigContainer, preserving the entire configuration tree, including circular
     * references. All sub-containers are recreated as mutable variants {@link LinkedConfigNode} and
     * {@link ArrayConfigList}, respectively. Only scalar types are left unchanged between the input and output, as
     * those cannot be made mutable.
     *
     * @return a mutable copy of this ConfigContainer
     */
    default @NotNull ConfigContainer mutableCopy() {
        return ConfigContainers.mutableCopy(this, LinkedConfigNode::new, ArrayConfigList::new);
    }

    /**
     * Creates a mutable copy of this ConfigContainer, preserving the entire configuration tree, including circular
     * references. All sub-containers are recreated as mutable variants supplied by the provided {@link IntFunction}s.
     * Only scalar types are left unchanged between the input and output, as those cannot be made mutable.
     *
     * @param configNodeCreator the function responsible for creating new, empty {@link ConfigNode} implementations;
     *                          cannot return {@code null}
     * @param configListCreator the function responsible for creating new, empty {@link ConfigList} implementations;
     *                          cannot return {@code null}
     * @return a mutable copy of this ConfigContainer
     */
    default @NotNull ConfigContainer mutableCopy(@NotNull IntFunction<? extends @NotNull ConfigNode> configNodeCreator,
        @NotNull IntFunction<? extends @NotNull ConfigList> configListCreator) {
        Objects.requireNonNull(configNodeCreator);
        Objects.requireNonNull(configListCreator);
        return ConfigContainers.mutableCopy(this, configNodeCreator, configListCreator);
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
