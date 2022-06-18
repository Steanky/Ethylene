package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

public class SimpleObjectBuilder implements ObjectBuilder {
    private final Object value;

    public SimpleObjectBuilder(Object value) {
        this.value = value;
    }

    @Override
    public void appendObject(Object parameter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Signature signature() {
        return null;
    }

    @Override
    public Object build() {
        return value;
    }
}
