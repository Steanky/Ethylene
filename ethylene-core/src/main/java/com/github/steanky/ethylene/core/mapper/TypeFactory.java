package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public interface TypeFactory {
    @NotNull Signature signature(@NotNull ConfigElement providedElement);

    @NotNull Object make(@NotNull Signature signature, @NotNull Object... objects);

    @FunctionalInterface
    interface Source {
        @NotNull TypeFactory factory(@NotNull Type type);
    }
}
