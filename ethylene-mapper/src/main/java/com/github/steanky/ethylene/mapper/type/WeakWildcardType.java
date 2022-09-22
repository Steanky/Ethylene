package com.github.steanky.ethylene.mapper.type;

import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Objects;

final class WeakWildcardType implements WeakType, WildcardType {
    private final Reference<Type>[] upperBoundReferences;
    private final String[] upperBoundNames;

    private final Reference<Type>[] lowerBoundReferences;
    private final String[] lowerBoundNames;

    @SuppressWarnings("unchecked")
    WeakWildcardType(@NotNull WildcardType wildcardType) {
        Objects.requireNonNull(wildcardType);

        if (wildcardType instanceof WeakWildcardType) {
            throw new IllegalArgumentException("Creating WeakWildcardType from WeakWildcardType");
        }

        Type[] upperBounds = wildcardType.getUpperBounds();
        this.upperBoundReferences = new Reference[upperBounds.length];
        this.upperBoundNames = new String[upperBounds.length];
        GenericInfoRepository.populate(upperBounds, upperBoundReferences, upperBoundNames);

        Type[] lowerBounds = wildcardType.getLowerBounds();
        this.lowerBoundReferences = new Reference[lowerBounds.length];
        this.lowerBoundNames = new String[lowerBounds.length];
        GenericInfoRepository.populate(lowerBounds, lowerBoundReferences, lowerBoundNames);
    }

    @Override
    public Type[] getUpperBounds() {
        return ReflectionUtils.resolve(upperBoundReferences, upperBoundNames, Type.class);
    }

    @Override
    public Type[] getLowerBounds() {
        return ReflectionUtils.resolve(lowerBoundReferences, lowerBoundNames, Type.class);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getUpperBounds()) ^ Arrays.hashCode(getLowerBounds());
    }

    @Override
    public boolean equals(Object obj) {
        Type[] upperBounds = getUpperBounds();
        Type[] lowerBounds = getLowerBounds();

        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof WildcardType other) {
            return Arrays.equals(upperBounds, other.getUpperBounds()) &&
                Arrays.equals(lowerBounds, other.getLowerBounds());
        }

        return false;
    }

    @Override
    public String toString() {
        return TypeUtils.toString(this);
    }

    @Override
    public @NotNull Class<?> getBoundClass() {
        return ReflectionUtils.rawType(this);
    }
}
