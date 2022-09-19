package com.github.steanky.ethylene.mapper.internal;

import com.github.steanky.ethylene.mapper.annotation.Name;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

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
}