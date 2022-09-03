package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.Entry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public abstract class CustomSignatureBase implements Signature {
    private final Collection<Entry<String, Type>> namedTypes;
    private final Type returnType;

    public CustomSignatureBase(@NotNull Collection<Entry<String, Type>> namedTypes, @NotNull Type returnType) {
        this.namedTypes = List.copyOf(namedTypes);
        this.returnType = Objects.requireNonNull(returnType);
    }

    @Override
    public @NotNull Iterable<Entry<String, Type>> argumentTypes() {
        return namedTypes;
    }

    @Override
    public ElementType typeHint() {
        return ElementType.NODE;
    }

    @Override
    public @NotNull Type returnType() {
        return returnType;
    }
}
