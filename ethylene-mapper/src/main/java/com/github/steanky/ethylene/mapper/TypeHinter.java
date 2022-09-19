package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public interface TypeHinter {
    @NotNull ElementType getHint(@NotNull Token<?> type);

    boolean assignable(@NotNull ConfigElement element, @NotNull Token<?> toType);
}
