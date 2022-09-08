package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public interface TypeResolver {
    @NotNull Type resolveType(@NotNull Type type, @Nullable ConfigElement configElement);

    void registerTypeImplementation(@NotNull Class<?> superclass, @NotNull Class<?> implementation);
}
