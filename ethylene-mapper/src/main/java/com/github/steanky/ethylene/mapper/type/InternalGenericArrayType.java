package com.github.steanky.ethylene.mapper.type;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Objects;

class InternalGenericArrayType implements GenericArrayType {
    private final Reference<Type> typeReference;
    private final String name;

    InternalGenericArrayType(@NotNull Type componentType) {
        this.typeReference = new WeakReference<>(componentType);
        this.name = componentType.getTypeName();
    }

    @Override
    public Type getGenericComponentType() {
        return Util.resolve(typeReference, name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(typeReference.get());
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof GenericArrayType && TypeUtils.equals(this, (GenericArrayType) obj);
    }

    @Override
    public String toString() {
        return TypeUtils.toString(this);
    }
}
