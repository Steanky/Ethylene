package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.collection.ConfigContainer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public interface BuilderResolver {
    @NotNull ObjectBuilder forType(@NotNull Type type, @NotNull ConfigContainer container);
}
