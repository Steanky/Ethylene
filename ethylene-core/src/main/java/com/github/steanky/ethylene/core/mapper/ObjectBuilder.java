package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public interface ObjectBuilder {
    void appendParameter(@NotNull ObjectBuilder parameter);

    default Object buildOrGetCurrent() {
        if(!isBuilding()) {
            return build();
        }

        return getCurrentObject();
    }

    Object build();

    Object getCurrentObject();

    Type @NotNull [] getArgumentTypes();

    boolean isBuilding();

    @NotNull TypeHinter.TypeHint typeHint();
}
