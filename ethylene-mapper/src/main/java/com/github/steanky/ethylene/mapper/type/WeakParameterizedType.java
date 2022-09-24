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
final class WeakParameterizedType implements ParameterizedType, WeakType {
    private final Reference<Class<?>> rawClassReference;
    private final String rawClassName;

    private final Reference<Type> ownerTypeReference;
    private final String ownerTypeName;

    private final Reference<Type>[] typeArgumentReferences;
    private final String[] typeArgumentNames;

    private final byte[] identifier;


    /**
     * Creates a new instance of this class.
     *
     * @param rawClass      the raw class of this generic type
     * @param owner         the owner, or enclosing, type
     * @param typeArguments the type arguments, which are not checked for compatibility with the number or bounds of the
     *                      raw class's type variables
     */
    @SuppressWarnings("unchecked")
    WeakParameterizedType(@NotNull Class<?> rawClass, @Nullable Type owner, Type @NotNull [] typeArguments) {
        this.rawClassReference = new WeakReference<>(rawClass);
        this.rawClassName = rawClass.getTypeName();

        this.ownerTypeReference = owner == null ? null : GenericInfo.ref(owner, this);
        this.ownerTypeName = owner == null ? StringUtils.EMPTY : owner.getTypeName();

        this.typeArgumentReferences = new Reference[typeArguments.length];
        this.typeArgumentNames = new String[typeArguments.length];
        GenericInfo.populate(typeArguments, typeArgumentReferences, typeArgumentNames, this);

        this.identifier = generateIdentifier(rawClass, owner, typeArguments);
    }

    static byte @NotNull [] generateIdentifier(@NotNull Class<?> rawClass, @Nullable Type owner,
        Type @NotNull [] typeArguments) {
        Type[] mergedArray = new Type[3 + typeArguments.length];
        mergedArray[0] = owner;
        mergedArray[1] = rawClass;
        mergedArray[2] = null;
        System.arraycopy(typeArguments, 0, mergedArray, 3, typeArguments.length);

        return GenericInfo.identifier(GenericInfo.PARAMETERIZED, mergedArray);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getActualTypeArguments()) ^
            Objects.hashCode(getOwnerType()) ^
            Objects.hashCode(getRawType());
    }

    @Override
    public boolean equals(Object obj) {
        //resolve the types first, in order to ensure consistent exception-throwing behavior
        //if we didn't do this, equality checking against null or reference equal objects could not throw exceptions,
        //even if the underlying type has been garbage collected
        Type rawType = getRawType();
        Type ownerType = getOwnerType();
        Type[] typeArguments = getActualTypeArguments();

        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof ParameterizedType other) {
            return Objects.equals(rawType, other.getRawType()) && Objects.equals(ownerType, other.getOwnerType()) &&
                Arrays.equals(typeArguments, other.getActualTypeArguments());
        }

        return false;
    }

    @Override
    public String toString() {
        return TypeUtils.toString(this);
    }

    @Override
    public Type[] getActualTypeArguments() {
        return ReflectionUtils.resolve(typeArgumentReferences, typeArgumentNames, Type.class);
    }

    @Override
    public Type getRawType() {
        return ReflectionUtils.resolve(rawClassReference, rawClassName);
    }

    @Override
    public Type getOwnerType() {
        return ownerTypeReference == null ? null : ReflectionUtils.resolve(ownerTypeReference, ownerTypeName);
    }

    @Override
    public @NotNull Class<?> getBoundClass() {
        return ReflectionUtils.resolve(rawClassReference, rawClassName);
    }

    @Override
    public byte[] identifier() {
        return identifier;
    }
}
