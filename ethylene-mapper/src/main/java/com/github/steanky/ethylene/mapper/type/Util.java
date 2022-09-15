package com.github.steanky.ethylene.mapper.type;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.reflect.Type;

class Util {
    static @NotNull Type resolve(@NotNull Reference<? extends Type> typeReference, @NotNull String typeName) {
        Type type = typeReference.get();
        if (type == null) {
            throw new TypeNotPresentException(typeName, null);
        }

        return type;
    }
}
