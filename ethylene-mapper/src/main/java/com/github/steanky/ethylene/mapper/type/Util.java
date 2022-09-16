package com.github.steanky.ethylene.mapper.type;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.reflect.Type;

@ApiStatus.Internal
public class Util {
    public static <TType extends Type> @NotNull TType resolve(@NotNull Reference<TType> typeReference, @NotNull String typeName) {
        TType type = typeReference.get();
        if (type == null) {
            throw new TypeNotPresentException(typeName, null);
        }

        return type;
    }
}
