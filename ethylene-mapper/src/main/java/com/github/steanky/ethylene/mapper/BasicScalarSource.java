package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.jetbrains.annotations.NotNull;

public class BasicScalarSource implements ScalarSource {
    public static final ScalarSource INSTANCE = new BasicScalarSource();

    protected BasicScalarSource() {}

    @Override
    public @NotNull ConfigElement make(@NotNull Object data) {
        return new ConfigPrimitive(data);
    }
}
