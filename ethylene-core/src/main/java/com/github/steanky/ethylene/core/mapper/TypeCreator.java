package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TypeCreator {
    Object createType(@NotNull Signature signature, @NotNull ConfigElement element, @NotNull Object[] arguments);
}
