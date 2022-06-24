package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ConstructorObjectBuilder implements ObjectBuilder {
    private final Constructor<?> constructor;
    private final Type[] args;
    private List<ObjectBuilder> parameters;
    private boolean isBuilding;

    public ConstructorObjectBuilder(@NotNull Constructor<?> constructor) {
        this.constructor = Objects.requireNonNull(constructor);
        this.args = constructor.getGenericParameterTypes();
    }

    @Override
    public void appendParameter(@NotNull ObjectBuilder parameter) {
        getParameters().add(parameter);
    }

    @Override
    public Object build() {
        Object[] args = new Object[parameters.size()];

        isBuilding = true;
        int i = 0;
        for(ObjectBuilder builder : parameters) {
            args[i++] = builder.buildOrGetCurrent();
        }
        isBuilding = false;

        try {
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new MappingException(e);
        }
    }

    @Override
    public Object getCurrentObject() {
        throw new MappingException("ConstructorObjectBuilder does not support circular references");
    }

    @Override
    public Type @NotNull [] getArgumentTypes() {
        return Arrays.copyOf(args, args.length);
    }

    @Override
    public boolean isBuilding() {
        return isBuilding;
    }

    @Override
    public @NotNull TypeHinter.TypeHint typeHint() {
        return TypeHinter.TypeHint.OBJECT;
    }

    private List<ObjectBuilder> getParameters() {
        if(parameters == null) {
            parameters = new ArrayList<>(constructor.getParameterCount());
        }

        return parameters;
    }
}
