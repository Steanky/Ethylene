package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.lang.reflect.Type;

public class ArrayTypeFactory extends ListFactoryBase {
    private final Class<?> rawType;

    public ArrayTypeFactory(@NotNull Type componentType) {
        super(componentType);
        this.rawType = TypeUtils.getRawType(componentType, null);
    }

    @Override
    public @NotNull Object make(@NotNull Signature signature, @NotNull ConfigElement providedElement,
            @NotNull Object... objects) {
        if (!providedElement.isList()) {
            throw new MapperException("expected ConfigList");
        }

        validateLengths(signature.elements().length, providedElement.asList().size(), objects.length);

        Object array = Array.newInstance(rawType, objects.length);
        for (int i = 0; i < objects.length; i++) {
            Array.set(array, i, objects[i]);
        }

        return array;
    }
}
