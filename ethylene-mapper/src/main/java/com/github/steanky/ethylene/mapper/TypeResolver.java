package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

@FunctionalInterface
public interface TypeResolver {
    @NotNull Type resolveType(@NotNull Type type, @Nullable ConfigElement configElement);
}