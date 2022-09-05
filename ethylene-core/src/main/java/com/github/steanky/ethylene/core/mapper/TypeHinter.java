package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public interface TypeHinter {
    @NotNull ElementType getHint(@NotNull Type type);

    @NotNull Type getPreferredType(@NotNull ConfigElement element, @NotNull Type type);
}
