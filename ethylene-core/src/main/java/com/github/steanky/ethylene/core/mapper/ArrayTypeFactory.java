package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;

public class ArrayTypeFactory extends ListFactoryBase {
    public ArrayTypeFactory(@NotNull Class<?> componentClass) {
        super(componentClass);
    }

    @Override
    public @NotNull Object make(@NotNull Signature signature, @NotNull ConfigElement providedElement,
            @NotNull Object... objects) {
        if (!providedElement.isList()) {
            throw new MapperException("expected ConfigList");
        }

        validateLengths(signature.elements().length, providedElement.asList().size(), objects.length);

        Object array = Array.newInstance((Class<?>)componentType, objects.length);
        for (int i = 0; i < objects.length; i++) {
            Array.set(array, i, objects[i]);
        }

        return array;
    }
}
