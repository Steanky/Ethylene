package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConstructorObjectBuilder implements ObjectBuilder {
    private final Constructor<?> constructor;
    private List<Object> parameters;

    public ConstructorObjectBuilder(@NotNull Constructor<?> constructor) {
        this.constructor = Objects.requireNonNull(constructor);
    }

    @Override
    public void appendParameter(Object parameter) {
        getParameters().add(parameter);
    }

    @Override
    public @NotNull Signature signature() {
        return null;
    }

    @Override
    public Object build() {
        return null;
    }

    private List<Object> getParameters() {
        if(parameters == null) {
            parameters = new ArrayList<>(constructor.getParameterCount());
        }

        return parameters;
    }
}
