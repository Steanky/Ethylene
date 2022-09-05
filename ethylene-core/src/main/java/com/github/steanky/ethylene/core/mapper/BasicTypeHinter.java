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
    public boolean assignable(@NotNull ConfigElement element, @NotNull Type toType) {
        return switch (element.type()) {
            case NODE -> getHint(toType) == ElementType.NODE;
            case LIST -> {
                ElementType hint = getHint(toType);
                //if toType is a map, collection, or array, returns true
                if (hint == ElementType.LIST) {
                    yield true;
                }

                //if toType is a superclass of Collection, returns true
                yield TypeUtils.isAssignable(Collection.class, toType);
            }
            case SCALAR -> {
                Object scalar = element.asScalar();
                if (scalar == null) {
                    //null is assignable to all types, even non-scalars
                    yield true;
                }

                yield TypeUtils.isAssignable(scalar.getClass(), toType);
            }
        };
    }
}
