package com.github.steanky.ethylene.mapper.type;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

class InternalParameterizedType implements ParameterizedType, CustomType {
    private final String rawName;
    private final String ownerName;

    private final Reference<Class<?>> raw;
    private final Reference<Type> owner;

    private final String[] typeArgumentNames;
    private final Reference<Type>[] typeArguments;

    @SuppressWarnings("unchecked")
    InternalParameterizedType(@NotNull Class<?> rawClass, @Nullable Type owner, Type @NotNull [] typeArguments) {
        this.rawName = rawClass.getTypeName();
        this.ownerName = owner == null ? StringUtils.EMPTY : owner.getTypeName();

        this.raw = new WeakReference<>(rawClass);
        this.owner = owner == null ? null : new WeakReference<>(owner);
        this.typeArguments = new Reference[typeArguments.length];
        this.typeArgumentNames = new String[typeArguments.length];

        for (int i = 0; i < typeArguments.length; i++) {
            Type type = typeArguments[i];
            this.typeArguments[i] = new WeakReference<>(type);
            this.typeArgumentNames[i] = type.getTypeName();
        }
    }

    @Override
    public Type[] getActualTypeArguments() {
        Type[] types = new Type[typeArguments.length];
        for (int i = 0; i < typeArguments.length; i++) {
            types[i] = Util.resolve(typeArguments[i], typeArgumentNames[i]);
        }

        return types;
    }

    @Override
    public Type getRawType() {
        return Util.resolve(raw, rawName);
    }

    @Override
    public Type getOwnerType() {
        return owner == null ? null : Util.resolve(owner, ownerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRawType(), getOwnerType(), Arrays.hashCode(getActualTypeArguments()));
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this ||
                obj instanceof ParameterizedType other && Objects.equals(getRawType(), other.getRawType()) &&
                        Objects.equals(getOwnerType(), other.getOwnerType()) &&
                        Arrays.equals(getActualTypeArguments(), other.getActualTypeArguments());
    }

    @Override
    public String toString() {
        return TypeUtils.toString(this);
    }
}
