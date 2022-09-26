package com.github.steanky.ethylene.mapper.internal;

import com.github.steanky.ethylene.mapper.annotation.Name;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.reflect.*;
import java.util.Objects;

/**
 * Contains some reflection-related utilities. These generally serve to supplement those already found in Apache
 * Commons.
 * <p>
 * This class and all of its methods are public to enable cross-package access, but must not be considered part of the
 * public API.
 */
public class ReflectionUtils {
    public static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

    /**
     * Extracts the name of a field. This is either its name as defined in the source code, or the value of a present
     * {@link Name} annotation. If such an annotation is present, its value is returned. Otherwise, it defaults to the
     * field name.
     *
     * @param field the field to extract a name from
     * @return the field's name
     */
    public static @NotNull String getFieldName(@NotNull Field field) {
        Name nameAnnotation = field.getDeclaredAnnotation(Name.class);
        return nameAnnotation == null ? field.getName() : nameAnnotation.value();
    }


    /**
     * Resolves the given reference to a Type. If it is not present, throws a {@link TypeNotPresentException}.
     *
     * @param typeReference the type reference
     * @param typeName      the type name
     * @param <TType>       the type object
     * @return the type itself
     */
    public static <TType extends Type> @NotNull TType resolve(@NotNull Reference<TType> typeReference,
        @NotNull String typeName) {
        TType type = typeReference.get();
        if (type == null) {
            throw new TypeNotPresentException(typeName, null);
        }

        return type;
    }

    @SuppressWarnings("unchecked")
    public static <TType extends Type> @NotNull TType[] resolve(@NotNull Reference<TType>[] typeReferences,
        @NotNull String[] names, @NotNull Class<?> type) {
        if (typeReferences.length != names.length) {
            throw new IllegalArgumentException("Reference array and name array must be the same length");
        }

        TType[] array = (TType[]) Array.newInstance(type, typeReferences.length);
        for (int i = 0; i < typeReferences.length; i++) {
            array[i] = resolve(typeReferences[i], names[i]);
        }

        return array;
    }

    /**
     * Returns the {@link ClassLoader} responsible for the given type. For class objects, this is the value returned by
     * {@link Class#getClassLoader()}. For {@link ParameterizedType}, this is the class loader of its raw type. For
     * {@link TypeVariable}, this is the classloader of the declaring class of the {@link GenericDeclaration} the
     * variable is part of. For {@link GenericArrayType} and {@link WildcardType}, this method returns {@code null}.
     *
     * @param type the type to retrieve a classloader from
     * @return the classloader of the type
     */
    public static @Nullable ClassLoader getClassLoader(@NotNull Type type) {
        Objects.requireNonNull(type);

        if (type instanceof Class<?> cls) {
            return cls.getClassLoader();
        }
        else if (type instanceof ParameterizedType parameterizedType) {
            return ((Class<?>)parameterizedType.getRawType()).getClassLoader();
        }
        else if (type instanceof TypeVariable<?> typeVariable) {
            return getOwner(typeVariable.getGenericDeclaration()).getClassLoader();
        }
        else if (type instanceof GenericArrayType || type instanceof WildcardType) {
            return null;
        }

        throw new IllegalArgumentException("Unexpected subclass of Type '" + type.getClass().getName() + "'");
    }

    public static @NotNull Class<?> rawType(@NotNull Type type) {
        if (type instanceof Class<?> cls) {
            return cls;
        }

        if (type instanceof ParameterizedType parameterizedType) {
            return (Class<?>) parameterizedType.getRawType();
        }

        if (type instanceof TypeVariable<?> typeVariable) {
            return rawType(typeVariable.getBounds()[0]);
        }

        if (type instanceof GenericArrayType genericArrayType) {
            return Array.newInstance(rawType(genericArrayType.getGenericComponentType()), 0).getClass();
        }

        if (type instanceof WildcardType wildcardType) {
            return rawType(wildcardType.getUpperBounds()[0]);
        }

        throw new IllegalArgumentException("Unexpected subclass of Type '" + type.getClass().getName() + "'");
    }

    public static @NotNull Class<?> getOwner(@NotNull GenericDeclaration genericDeclaration) {
        if (genericDeclaration instanceof Class<?> cls) {
            return cls;
        }
        else if(genericDeclaration instanceof Executable executable) {
            return executable.getDeclaringClass();
        }
        else {
            throw new IllegalArgumentException("Unexpected subclass of GenericDeclaration '" + genericDeclaration
                .getClass().getName() + "'");
        }
    }
}
