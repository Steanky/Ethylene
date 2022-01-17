package com.github.steanky.ethylene.core.databind;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;

/**
 * Used to preserve generic type information at runtime.
 * @param <T> the type of object whose generic information will be retained
 */
@SuppressWarnings("unused")
public abstract class TypeToken<T> {
    private final Type type;
    private final Class<? super T> rawType;

    /**
     * Creates a new instance of TypeToken. This method is to be invoked solely through subclasses.
     * @throws IllegalStateException if no generic type parameters are given
     */
    protected TypeToken() {
        Type superclass = getClass().getGenericSuperclass();
        if(superclass instanceof ParameterizedType parameterizedType) {
            type = parameterizedType.getActualTypeArguments()[0];

            //noinspection unchecked
            rawType = (Class<? super T>) getRawType(type);
        }
        else {
            throw new IllegalStateException("TypeToken must be instantiated with a generic type parameter");
        }
    }

    /**
     * Returns the underlying type, containing generic type information.
     * @return the underlying type
     */
    public final @NotNull Type getType() {
        return type;
    }

    public final @NotNull Class<? super T> getRawType() {
        return rawType;
    }

    private static Class<?> getRawType(Type type) {
        if (type instanceof Class<?> typeClass) {
            return typeClass;
        }
        else if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if(rawType instanceof Class<?> rawClass) {
                return rawClass;
            }

            throw new IllegalArgumentException("Raw type not instance of Class: " + rawType);
        }
        else if (type instanceof TypeVariable) {
            return Object.class;
        }
        else if (type instanceof GenericArrayType arrayType) {
            return Array.newInstance(getRawType(arrayType.getGenericComponentType()), 0).getClass();
        }
        else if (type instanceof WildcardType wildcardType) {
            return getRawType(wildcardType.getUpperBounds()[0]);
        }
        else {
            throw new IllegalArgumentException("Unexpected type: " + type.getTypeName());
        }
    }
}
