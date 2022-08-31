package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

@FunctionalInterface
public interface TypeHinter {
    @NotNull Hint getHint(@NotNull Type type);

    enum Hint {
        CONTAINER_LIKE, OBJECT_LIKE, SCALAR;

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean compatible(@NotNull ConfigElement element) {
            return switch (this) {
                case CONTAINER_LIKE -> element.isList();
                case OBJECT_LIKE -> element.isNode();
                case SCALAR -> element.isScalar();
            };
        }
    }
}
