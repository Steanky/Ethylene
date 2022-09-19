package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
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
}
