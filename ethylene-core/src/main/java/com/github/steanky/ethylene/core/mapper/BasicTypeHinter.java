package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class BasicTypeHinter implements TypeHinter {
    @Override
    public @NotNull ElementType getHint(@NotNull Type type) {
        if (TypeUtils.isAssignable(type, Map.class) || TypeUtils.isAssignable(type, Collection.class) ||
                TypeUtils.isArrayType(type)) {
            return ElementType.LIST;
        } else if (ClassUtils.isPrimitiveOrWrapper(TypeUtils.getRawType(type, null)) ||
                TypeUtils.isAssignable(type, String.class)) {
            return ElementType.SCALAR;
        }

        return ElementType.NODE;
    }

    @Override
    public @NotNull Type getPreferredType(@NotNull ConfigElement element, @NotNull Type target) {
        return switch (element.type()) {
            case NODE, LIST -> target;
            case SCALAR -> {
                Object scalar = element.asScalar();
                if (scalar == null) {
                    yield Object.class;
                }

                yield scalar.getClass();
            }
        };
    }
}
