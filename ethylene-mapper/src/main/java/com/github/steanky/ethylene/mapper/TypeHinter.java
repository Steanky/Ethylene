package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

/**
 * Provides basic inspections on {@link Token}s and {@link ConfigElement}s.
 */
public interface TypeHinter {
    /**
     * Identifies the {@link ElementType} that the given token should be converted to.
     *
     * @param type the token to inspect
     * @return the ElementType the token corresponds to
     */
    @NotNull ElementType getHint(@NotNull Token<?> type);

    /**
     * Determines if the given {@link ConfigElement} may be assigned to the given type.
     *
     * @param element the element to check for assignability to the given type
     * @param toType  the type
     * @return true if the element is assignable to the given type; false otherwise
     */
    boolean assignable(@NotNull ConfigElement element, @NotNull Token<?> toType);

    /**
     * Calculates the "preferred type" of the given {@link ConfigElement}, given a {@link Token}.
     *
     * @param element the element for which to calculate a preferred type
     * @param type    the type the element being assigned to
     * @return the preferred type of this element
     */
    @NotNull Token<?> getPreferredType(@NotNull ConfigElement element, @NotNull Token<?> type);
}
