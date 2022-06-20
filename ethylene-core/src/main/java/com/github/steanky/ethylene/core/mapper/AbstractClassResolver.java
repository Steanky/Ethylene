package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

public interface AbstractClassResolver {
    @NotNull Class<?> resolveAbstract(@NotNull Class<?> abstractClass);
}
