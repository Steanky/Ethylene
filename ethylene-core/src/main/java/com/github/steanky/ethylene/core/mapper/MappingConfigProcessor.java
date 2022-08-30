package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MappingConfigProcessor<T> implements ConfigProcessor<T> {
    private final Token<T> token;
    private final TypeResolver typeResolver;

    public MappingConfigProcessor(@NotNull Token<T> token, @NotNull TypeResolver typeResolver) {
        this.token = Objects.requireNonNull(token);
        this.typeResolver = Objects.requireNonNull(typeResolver);
    }

    @Override
    public T dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {

        return null;
    }

    @Override
    public @NotNull ConfigElement elementFromData(T t) throws ConfigProcessException {
        return null;
    }
}
