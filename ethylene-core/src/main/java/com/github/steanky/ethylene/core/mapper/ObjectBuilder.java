package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

public interface ObjectBuilder {
    void appendParameter(Object parameter);

    @NotNull Signature signature();

    Object build();
}
