package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public interface TypeHinter {
    enum Hint {
        CONTAINER,
        OBJECT,
        SCALAR;

        public boolean compatible(@NotNull ConfigElement element) {
            return switch (this) {
                case CONTAINER -> element.isList();
                case OBJECT -> element.isNode();
                case SCALAR -> element.isScalar();
            };
        }
    }

    @NotNull Hint getHint(@NotNull Type type);
}
