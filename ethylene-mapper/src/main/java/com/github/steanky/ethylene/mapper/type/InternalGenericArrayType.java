package com.github.steanky.ethylene.mapper.type;

import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
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
        return ReflectionUtils.resolve(typeReference, typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(typeReference.get());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof InternalGenericArrayType other) {
            return Objects.equals(typeReference.get(), other.typeReference.get());
        }

        if (obj instanceof GenericArrayType other) {
            return Objects.equals(typeReference.get(), other.getGenericComponentType());
        }

        return false;
    }

    @Override
    public String toString() {
        return TypeUtils.toString(this);
    }
}
