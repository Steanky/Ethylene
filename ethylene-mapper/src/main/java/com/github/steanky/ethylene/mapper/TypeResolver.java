package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface TypeResolver {
    @NotNull Token<?> resolveType(@NotNull Token<?> type, @Nullable ConfigElement configElement);
}