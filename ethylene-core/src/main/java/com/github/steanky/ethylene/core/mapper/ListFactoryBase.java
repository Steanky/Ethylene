package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Objects;

public abstract class ListFactoryBase extends TypeFactoryBase {
    protected final Type componentType;

    public ListFactoryBase(@NotNull Type componentType) {
        this.componentType = Objects.requireNonNull(componentType);
    }

    @Override
    public @NotNull Signature signature(@NotNull ConfigElement providedElement) {
        if (!providedElement.isList()) {
            throw new MapperException("expected ConfigList");
        }

        SignatureElement[] elements = new SignatureElement[providedElement.asList().size()];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = new SignatureElement(componentType, i);
        }

        return new Signature(0, true, elements);
    }
}
