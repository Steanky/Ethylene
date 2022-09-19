package com.github.steanky.ethylene.mapper.internal;

import com.github.steanky.ethylene.mapper.annotation.Name;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

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
     * Determines the raw type of a given {@link Type} object, with similar semantics to
     * {@link TypeUtils#getRawType(Type, Type)}, when the second parameter is passed a null value. However, in all
     * instances where that method would return null, this method throws an {@link IllegalArgumentException} instead.
     *
     * @param type the type to convert to a raw class
     * @return the raw type
     * @throws IllegalArgumentException if Type is a {@link WildcardType} or {@link TypeVariable}
     */
    public static @NotNull Class<?> rawType(@NotNull Type type) {
        Class<?> cls = TypeUtils.getRawType(type, null);
        if (cls == null) {
            throw new IllegalArgumentException("Invalid type '" + type.getTypeName() + "'");
        }

        return cls;
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
}
