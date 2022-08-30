package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Objects;

public class BasicTypeFactorySource implements TypeFactory.Source {
    private final TypeHinter typeHinter;

    public BasicTypeFactorySource(@NotNull TypeHinter typeHinter) {
        this.typeHinter = Objects.requireNonNull(typeHinter);
    }

    @Override
    public @NotNull TypeFactory factory(@NotNull Type type) {
        TypeHinter.Hint hint = typeHinter.getHint(type);
        switch (hint) {

        }
        return null;
    }
}
