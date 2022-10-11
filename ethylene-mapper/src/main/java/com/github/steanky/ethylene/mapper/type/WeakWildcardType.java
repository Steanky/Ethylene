package com.github.steanky.ethylene.mapper.type;

import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * Weak version of {@link WildcardType} which does not retain any strong references to its bounds. Not part of the
 * public API.
 */
final class WeakWildcardType extends WeakTypeBase implements WildcardType, WeakType {
    private final Reference<Type>[] upperBoundReferences;
    private final String[] upperBoundNames;

    private final Reference<Type>[] lowerBoundReferences;
    private final String[] lowerBoundNames;

    /**
     * Creates a new instance of this class from the given {@link WildcardType}. The new instance will have the same
     * bounds as the provided type.
     *
     * @param wildcardType the wildcard type from which to create this instance
     */
    @SuppressWarnings("unchecked")
    WeakWildcardType(@NotNull WildcardType wildcardType) {
        super(generateIdentifier(wildcardType.getUpperBounds(), wildcardType.getLowerBounds()));

        Type[] upperBounds = wildcardType.getUpperBounds();
        this.upperBoundReferences = new Reference[upperBounds.length];
        this.upperBoundNames = new String[upperBounds.length];
        GenericInfo.populate(upperBounds, upperBoundReferences, upperBoundNames, this, null);

        Type[] lowerBounds = wildcardType.getLowerBounds();
        this.lowerBoundReferences = new Reference[lowerBounds.length];
        this.lowerBoundNames = new String[lowerBounds.length];
        GenericInfo.populate(lowerBounds, lowerBoundReferences, lowerBoundNames, this, null);
    }

    private static byte[] generateIdentifier(Type[] upperBounds, Type[] lowerBounds) {
        Type[] identifierTypes = new Type[upperBounds.length + lowerBounds.length + 1];
        System.arraycopy(upperBounds, 0, identifierTypes, 0, upperBounds.length);
        //add null type as separator to indicate boundary between upper and lower bounds
        identifierTypes[upperBounds.length] = null;
        System.arraycopy(lowerBounds, 0, identifierTypes, upperBounds.length + 1, lowerBounds.length);
        return GenericInfo.identifier(GenericInfo.WILDCARD, identifierTypes);
    }

    @Override
    public Type[] getUpperBounds() {
        return ReflectionUtils.resolve(upperBoundReferences, upperBoundNames, Type.class);
    }

    @Override
    public Type[] getLowerBounds() {
        return ReflectionUtils.resolve(lowerBoundReferences, lowerBoundNames, Type.class);
    }
}
