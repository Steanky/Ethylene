package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ScalarSource {
    @NotNull ConfigElement makeElement(@Nullable Object data, @NotNull Token<?> type);

    @Nullable Object makeObject(@NotNull ConfigElement element, @NotNull Token<?> type);
}
