package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves types into concrete implementations. If no type resolution is available, a {@link MapperException} will be
 * thrown.
 */
@FunctionalInterface
public interface TypeResolver {
    /**
     * Resolves the given type. If an implementation is available, and it is compatible with the given
     * {@link ConfigElement}, it is returned. If no implementation is available, a type is derived from the
     * ConfigElement alone. If this derived type is not assignable to {@code type}, a {@link MapperException} is
     * thrown.
     *
     * @param type          the type to resolve
     * @param configElement the {@link ConfigElement} used to derive a type if there is no available implementation; can
     *                      be null if no element is known
     * @return the resolved type
     */
    @NotNull Token<?> resolveType(@NotNull Token<?> type, @Nullable ConfigElement configElement);
}