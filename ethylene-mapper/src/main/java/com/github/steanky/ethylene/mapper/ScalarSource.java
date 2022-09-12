package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

public interface ScalarSource {
    @NotNull ConfigElement make(@NotNull Object data);
}