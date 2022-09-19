package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

public class BasicTypeHinter implements TypeHinter {
    public static final TypeHinter INSTANCE = new BasicTypeHinter();

    protected BasicTypeHinter() {
    }

    @Override
    public @NotNull ElementType getHint(@NotNull Token<?> type) {
        if (type.isSubclassOf(Map.class) || type.isSubclassOf(Collection.class) || type.isArrayType()) {
            return ElementType.LIST;
        } else if (type.isPrimitiveOrWrapper() || type.isSubclassOf(String.class)) {
            return ElementType.SCALAR;
        }

        return ElementType.NODE;
    }

    @Override
    public boolean assignable(@NotNull ConfigElement element, @NotNull Token<?> toType) {
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
                yield toType.isSuperclassOf(Collection.class);
            }
            case SCALAR -> {
                Object scalar = element.asScalar();
                if (scalar == null) {
                    //null is assignable to all types, even non-scalars
                    yield true;
                }

                //simple assignability check
                yield toType.isSuperclassOf(scalar.getClass());
            }
        };
    }
}
