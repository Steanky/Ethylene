package com.github.steanky.ethylene.core.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;

public final class ReflectionUtils {
    private ReflectionUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>Attempts to resolve the given Type into a corresponding class object.</p>
     *
     * <p>This method essentially performs a form of type erasure; ex. the generic type {@code List<String>} becomes the
     * class {@code List}, and so on. For more complex type declarations, such as bounded wildcards, the type of the
     * upper bound is the type returned by this method. For example, the wildcard type declaration
     * {@code ? extends String} resolves to String. Wildcards that do not supply an upper bound will resolve to Object,
     * as in {@code ? super String} or simply ?. Generic array types are handled as follows: {@code List<?>[]} ->
     * {@code List[]}. Furthermore, this method can correctly resolve "inheritance chains" of type variables, as well as
     * multidimensional arrays.</p>
     *
     * <p>Subclasses of Type that are not themselves subclasses or instances of Class, ParameterizedType, WildcardType,
     * GenericArrayType, or TypeVariable are not supported. Attempting to resolve these types will result in an
     * IllegalArgumentException.</p>
     * @param type the type to resolve into a class
     * @return the corresponding class
     */
    public static @NotNull Class<?> getUnderlyingClass(@NotNull Type type) {
        if(type instanceof Class<?> clazz) {
            return clazz;
        }
        else if(type instanceof ParameterizedType parameterizedType) {
            //the JDK implementation of this class, ParameterizedTypeImpl, *always* returns a Class<?>, but third-party
            //subclasses are not obligated to do so by method contract
            Type rawType = parameterizedType.getRawType();
            if(rawType instanceof Class<?> clazz) {
                return clazz;
            }

            //try to handle non-class raw types in a sane way
            return getUnderlyingClass(rawType);
        }
        else if(type instanceof WildcardType wildcardType) {
            Type[] upperBounds = wildcardType.getUpperBounds();

            //JDK documentation requests that users of this API accommodate more than one upper bound, although as of
            //6/17/2022, JDK 17 such a condition is not possible, so just use the first bound
            if(upperBounds.length > 0) {
                return getUnderlyingClass(upperBounds[0]);
            }

            //with no upper bound, we can't assume anything about the type (lower bounds are not helpful)
            return Object.class;
        }
        else if(type instanceof GenericArrayType arrayType) {
            //make sure we preserve the array type
            return getUnderlyingClass(arrayType.getGenericComponentType()).arrayType();
        }
        else if(type instanceof TypeVariable<?> typeVariable) {
            //TypeVariable only supplies upper bounds
            Type[] upperBounds = typeVariable.getBounds();
            if(upperBounds.length > 0) {
                return getUnderlyingClass(upperBounds[0]);
            }

            return Object.class;
        }

        throw new IllegalArgumentException("Unexpected subclass of Type: " + type.getClass().getTypeName());
    }
}
