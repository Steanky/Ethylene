package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public class ScalarObjectBuilder implements ObjectBuilder {
    private final Object value;

    public ScalarObjectBuilder(Object value) {
        this.value = value;
    }

    @Override
    public void appendParameter(@NotNull ObjectBuilder parameter) {
        throw new MappingException("This object is already complete");
    }

    @Override
    public Object build() {
        return value;
    }

    @Override
    public Object getCurrentObject() {
        throw new MappingException("This builder has no current object");
    }

    @Override
    public Type @NotNull [] getArgumentTypes() {
        return new Type[0];
    }

    @Override
    public boolean isBuilding() {
        return false;
    }

    @Override
    public @NotNull TypeHinter.TypeHint typeHint() {
        return TypeHinter.TypeHint.SCALAR;
    }
}
