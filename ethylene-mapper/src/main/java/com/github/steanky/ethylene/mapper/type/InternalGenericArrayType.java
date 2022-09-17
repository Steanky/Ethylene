package com.github.steanky.ethylene.mapper.type;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Implementation of {@link GenericArrayType} that retains no strong references to its underlying component {@link Type}
 * object. Not part of the public API.
 */
class InternalGenericArrayType implements GenericArrayType, CustomType {
    private final Reference<Type> typeReference;
    private final String typeName;

    /**
     * Creates a new instance of this class from the given component type.
     * @param componentType the component type
     */
    InternalGenericArrayType(@NotNull Type componentType) {
        this.typeReference = new WeakReference<>(Objects.requireNonNull(componentType));
        this.typeName = componentType.getTypeName();
    }

    @Override
    public Type getGenericComponentType() {
        return Util.resolve(typeReference, typeName);
    }

    @Override
    public int hashCode() {
        return getGenericComponentType().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof GenericArrayType other &&
                getGenericComponentType().equals(other.getGenericComponentType());
    }

    @Override
    public String toString() {
        return TypeUtils.toString(this);
    }
}
