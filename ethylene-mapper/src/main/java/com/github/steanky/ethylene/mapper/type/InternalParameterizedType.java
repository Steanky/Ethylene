package com.github.steanky.ethylene.mapper.type;

import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
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

/**
 * Implementation of {@link ParameterizedType} that retains no strong references to any {@link Type} or {@link Class}
 * objects used in constructing it. Not part of the public API.
 */
class InternalParameterizedType implements ParameterizedType, CustomType {
    private final String rawName;
    private final String ownerName;

    private final Reference<Class<?>> raw;
    private final Reference<Type> owner;

    private final String[] typeArgumentNames;
    private final Reference<Type>[] typeArguments;

    /**
     * Creates a new instance of this class.
     *
     * @param rawClass the raw class of this generic type
     * @param owner the owner, or enclosing, class
     * @param typeArguments the type arguments, which are not checked for compatibility with the number or bounds of the
     *                      raw class's type variables
     */
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

    private Type[] getTypeArgumentArray() {
        Type[] types = new Type[typeArguments.length];
        for (int i = 0; i < typeArguments.length; i++) {
            types[i] = typeArguments[i].get();
        }

        return types;
    }

    @Override
    public Type[] getActualTypeArguments() {
        Type[] types = new Type[typeArguments.length];
        for (int i = 0; i < typeArguments.length; i++) {
            types[i] = ReflectionUtils.resolve(typeArguments[i], typeArgumentNames[i]);
        }

        return types;
    }

    @Override
    public Type getRawType() {
        return ReflectionUtils.resolve(raw, rawName);
    }

    @Override
    public Type getOwnerType() {
        return owner == null ? null : ReflectionUtils.resolve(owner, ownerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw.get(), owner.get(), Arrays.hashCode(getTypeArgumentArray()));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof InternalParameterizedType other) {
            //directly compare referents to avoid throwing exceptions if the underlying types have been disposed of
            //public methods will throw exceptions if this has occurred
            return Objects.equals(raw.get(), other.raw.get()) && Objects.equals(owner.get(), other.owner.get()) &&
                    Arrays.equals(getTypeArgumentArray(), other.getTypeArgumentArray());
        }

        if (obj instanceof ParameterizedType other) {
            //since we aren't comparing against another InternalParameterizedType, only use the interface methods
            return Objects.equals(raw.get(), other.getRawType()) && Objects.equals(owner.get(), other.getOwnerType()) &&
                    Arrays.equals(getTypeArgumentArray(), other.getActualTypeArguments());
        }

        return false;
    }

    @Override
    public String toString() {
        return TypeUtils.toString(this);
    }
}
