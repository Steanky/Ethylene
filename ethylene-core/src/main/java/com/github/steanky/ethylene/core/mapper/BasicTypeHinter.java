package com.github.steanky.ethylene.core.mapper;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class BasicTypeHinter implements TypeHinter {
    @Override
    public @NotNull Hint getHint(@NotNull Type type) {
        if (TypeUtils.isAssignable(type, Map.class) || TypeUtils.isAssignable(type, Collection.class) ||
                TypeUtils.isArrayType(type)) {
            return Hint.CONTAINER_LIKE;
        } else if (ClassUtils.isPrimitiveOrWrapper(TypeUtils.getRawType(type, null)) ||
                TypeUtils.isAssignable(type, String.class)) {
            return Hint.SCALAR;
        }

        return Hint.OBJECT_LIKE;
    }
}
