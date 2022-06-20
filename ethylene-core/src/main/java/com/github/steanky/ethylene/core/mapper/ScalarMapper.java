package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public interface ScalarMapper {
    Object convertScalar(@NotNull Type type, @NotNull ConfigElement element);
}
