package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public interface TypeHinter {
    @NotNull Hint getHint(@NotNull Type type);

    enum Hint {
        COLLECTION_LIKE, MAP_LIKE, ARRAY_LIKE, OBJECT, SCALAR;

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean compatible(@NotNull ConfigElement element) {
            return switch (this) {
                case COLLECTION_LIKE, MAP_LIKE, ARRAY_LIKE -> element.isList();
                case OBJECT -> element.isNode();
                case SCALAR -> element.isScalar();
            };
        }
    }
}
