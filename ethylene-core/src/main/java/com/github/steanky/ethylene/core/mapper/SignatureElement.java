package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public interface SignatureElement {
    @NotNull Type getType();

    @NotNull String getName();

    boolean isPrimitive();
}
