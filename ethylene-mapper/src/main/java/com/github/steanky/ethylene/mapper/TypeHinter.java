package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/**
 * Provides basic inspections on {@link Token}s and {@link ConfigElement}s.
 */
public interface TypeHinter {
    @NotNull ElementType getHint(@NotNull Token<?> type);

    boolean assignable(@NotNull ConfigElement element, @NotNull Token<?> toType);

    @NotNull Token<?> getPreferredType(@NotNull ConfigElement element, @NotNull Token<?> type);
}
