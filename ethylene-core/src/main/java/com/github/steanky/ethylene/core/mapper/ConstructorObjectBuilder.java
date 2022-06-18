package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.processor.ConfigProcessException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Objects;

public class ConstructorObjectBuilder implements ObjectBuilder {
    private final Constructor<?> constructor;

    public ConstructorObjectBuilder(@NotNull Constructor<?> constructor) {
        this.constructor = Objects.requireNonNull(constructor);
    }

    @Override
    public @NotNull Object construct(Object... objects) throws ConfigProcessException {
        try {
            return constructor.newInstance(objects);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ConfigProcessException(e);
        }
    }

    @Override
    public @NotNull Signature signature() {
        return null;
    }
}
