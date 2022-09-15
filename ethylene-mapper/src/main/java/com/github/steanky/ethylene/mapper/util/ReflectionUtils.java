package com.github.steanky.ethylene.mapper.util;

import com.github.steanky.ethylene.mapper.annotation.Name;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

public class ReflectionUtils {
    private static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

    public static @NotNull Type[] extractGenericTypeParameters(@NotNull Type type,
            @NotNull Class<?> genericSuperclass) {
        TypeVariable<?>[] typeVariables = genericSuperclass.getTypeParameters();
        if (typeVariables.length == 0) {
            return EMPTY_TYPE_ARRAY;
        }

        Type[] params = new Type[typeVariables.length];
        Map<TypeVariable<?>, Type> typeMap = TypeUtils.getTypeArguments(type, genericSuperclass);
        for (int i = 0; i < typeVariables.length; i++) {
            Type typeParameter = typeMap.get(typeVariables[i]);
            if (typeParameter instanceof TypeVariable<?> || typeParameter == null) {
                typeParameter = Object.class;
            }

            params[i] = typeParameter;
        }

        return params;
    }

    public static @NotNull String getFieldName(@NotNull Field field) {
        Name nameAnnotation = field.getDeclaredAnnotation(Name.class);
        return nameAnnotation == null ? field.getName() : nameAnnotation.value();
    }

    public static @NotNull Class<?> rawType(@NotNull Type type) {
        Class<?> cls = TypeUtils.getRawType(type, null);
        if (cls == null) {
            throw new IllegalArgumentException("Invalid type '" + type.getTypeName() + "'");
        }

        return cls;
    }
}
