package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * Represents a {@link ConfigElement} capable of holding other ConfigElements. ConfigContainer objects are generally
 * also either {@link Map} or {@link Collection} implementations.
 */
public interface ConfigContainer extends ConfigElement {
    @Override
    default boolean isContainer() {
        return true;
    }

    @Override
    default @NotNull ConfigContainer asContainer() {
        return this;
    }

    /**
     * Returns the <i>entry collection</i> maintained by this ConfigContainer. The collection must be immutable and
     * read-through. Additionally, repeated calls to this method should return the same instance.
     *
     * @return an immutable, read-through collection representing the entries contained in this object
     */
    @NotNull Collection<ConfigEntry> entryCollection();

    @NotNull Collection<ConfigElement> elementCollection();
}
