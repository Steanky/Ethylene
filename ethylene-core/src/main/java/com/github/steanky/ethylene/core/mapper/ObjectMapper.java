package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import org.jetbrains.annotations.NotNull;

public interface ObjectMapper {
    Object construct(@NotNull Token<?> token, @NotNull ConfigElement data) throws ConfigProcessException;

    @NotNull ConfigElement deconstruct(@NotNull Token<?> token, Object data) throws ConfigProcessException;
}
