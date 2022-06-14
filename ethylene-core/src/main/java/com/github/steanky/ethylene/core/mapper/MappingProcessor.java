package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MappingProcessor<T> implements ConfigProcessor<T> {
    private final Token<?> root;
    private final ObjectMapper mapper;


    public MappingProcessor(@NotNull Token<T> token, @NotNull ObjectMapper mapper) {
        this.root = Objects.requireNonNull(token);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public T dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException {
        //noinspection unchecked
        return (T) mapper.construct(root, element);
    }

    @Override
    public @NotNull ConfigElement elementFromData(T o) throws ConfigProcessException {
        return mapper.deconstruct(root, o);
    }
}
