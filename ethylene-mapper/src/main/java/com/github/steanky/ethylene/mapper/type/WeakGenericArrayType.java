package com.github.steanky.ethylene.mapper.type;

import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Implementation of {@link GenericArrayType} that retains no strong references to its underlying component {@link Type}
 * object. Not part of the public API.
 */
final class WeakGenericArrayType implements GenericArrayType, WeakType {
    private final Reference<Type> componentTypeReference;
    private final String componentTypeName;

    private final byte[] identifier;

    /**
     * Creates a new instance of this class from the given component type.
     *
     * @param componentType the component type
     */
    WeakGenericArrayType(@NotNull Type componentType) {
        this.componentTypeReference = GenericInfo.ref(componentType, this);
        this.componentTypeName = componentType.getTypeName();

        this.identifier = generateIdentifier(componentType);
    }

    static byte @NotNull [] generateIdentifier(@NotNull Type componentType) {
        return GenericInfo.identifier(GenericInfo.GENERIC_ARRAY, componentType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getGenericComponentType());
    }

    @Override
    public boolean equals(Object obj) {
        Type thisType = getGenericComponentType();

        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof GenericArrayType other) {
            return Objects.equals(thisType, other.getGenericComponentType());
        }

        return false;
    }

    @Override
    public String toString() {
        return TypeUtils.toString(this);
    }

    @Override
    public Type getGenericComponentType() {
        return ReflectionUtils.resolve(componentTypeReference, componentTypeName);
    }

    @Override
    public @NotNull ClassLoader getBoundClassloader() {
        return ReflectionUtils.rawType(this).getClassLoader();
    }

    @Override
    public byte[] identifier() {
        return identifier;
    }
}
