package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

@FunctionalInterface
public interface TypeResolver {
    @NotNull Class<?> resolveType(@NotNull Type type);
}
