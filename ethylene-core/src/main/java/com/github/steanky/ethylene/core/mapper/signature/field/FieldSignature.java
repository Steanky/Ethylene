package com.github.steanky.ethylene.core.mapper.signature.field;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;

public class FieldSignature implements Signature {
    private final Type type;

    private Collection<Entry<String, Type>> types;

    public FieldSignature(@NotNull Type type) {
        this.type = Objects.requireNonNull(type);
    }

    @Override
    public @NotNull Iterable<Entry<String, Type>> argumentTypes() {
        return null;
    }

    @Override
    public Object makeObject(@NotNull Object[] args) {
        return null;
    }

    @Override
    public boolean hasArgumentNames() {
        return true;
    }

    @Override
    public int length(@NotNull ConfigElement element) {
        return 0;
    }

    @Override
    public @NotNull ElementType typeHint() {
        return ElementType.NODE;
    }

    @Override
    public @NotNull Type returnType() {
        return type;
    }
}
