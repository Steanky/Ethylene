package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

public interface ScalarMapper {
    @NotNull Object makeScalar(@NotNull ConfigElement element);
}
