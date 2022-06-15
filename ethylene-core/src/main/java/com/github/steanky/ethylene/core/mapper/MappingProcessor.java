package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MappingProcessor<T> implements ConfigProcessor<T> {
    private final Token<?> root;

    public MappingProcessor(@NotNull Token<T> token) {
        this.root = Objects.requireNonNull(token);
    }

    @Override
    public T dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
        return null;
    }

    @Override
    public @NotNull ConfigElement elementFromData(T o) throws ConfigProcessException {
        return null;
    }
}
