package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a source of scalar objects and elements.
 */
public interface ScalarSource {
    /**
     * Creates a {@link ConfigElement} from a given, nullable object and provided upper bounds.
     *
     * @param data the data object from which to construct an element
     * @param upperBounds the upper bounds of the data type (may contain additional generic information)
     * @return a new ConfigElement from the provided data object
     */
    @NotNull ConfigElement makeElement(@Nullable Object data, @NotNull Token<?> upperBounds);

    /**
     * Creates an {@link Object} from a given {@link ConfigElement} and provided upper bounds.
     *
     * @param element the element from which to create an object
     * @param upperBounds the upper bounds of the data type (may contain additional generic information)
     * @return a new Object from the provided element
     */
    @Nullable Object makeObject(@NotNull ConfigElement element, @NotNull Token<?> upperBounds);
}
