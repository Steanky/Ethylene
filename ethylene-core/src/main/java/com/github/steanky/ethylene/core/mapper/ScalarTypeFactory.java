package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Objects;

public class ScalarTypeFactory extends TypeFactoryBase {
    private final Type scalarType;

    public ScalarTypeFactory(@NotNull Type scalarType) {
        this.scalarType = Objects.requireNonNull(scalarType);
    }

    @Override
    public @NotNull Signature signature(@NotNull ConfigElement providedElement) {
        if (!providedElement.isScalar()) {
            throw new MapperException("expected scalar");
        }

        return new Signature(0, new SignatureElement[] { new SignatureElement(scalarType, 0) });
    }

    @Override
    public @NotNull Object make(@NotNull Signature signature, @NotNull ConfigElement providedElement,
            @NotNull Object... objects) {
        if (!providedElement.isScalar()) {
            throw new MapperException("expected scalar");
        }

        validateLengths(signature.elements().length, 0, objects.length);
        return objects[0];
    }
}
