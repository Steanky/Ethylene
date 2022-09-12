package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class BasicTypeHinter implements TypeHinter {
    public static final TypeHinter INSTANCE = new BasicTypeHinter();

    protected BasicTypeHinter() {}

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
            //simplest case: if toType is a LIST or SCALAR and the element isn't, we are not assignable
            case NODE -> getHint(toType) == ElementType.NODE;
            case LIST -> {
                ElementType hint = getHint(toType);
                if (hint == ElementType.LIST) {
                    //we know we're a map, collection, or array, so we're compatible
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

                //simple assignability check
                yield TypeUtils.isAssignable(scalar.getClass(), toType);
            }
        };
    }
}
