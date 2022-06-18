package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public interface ObjectBuilder {
    void appendObject(Object parameter);

    @NotNull Signature signature();

    Object build();
}
