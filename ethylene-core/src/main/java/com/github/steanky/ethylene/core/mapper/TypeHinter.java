package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public interface TypeHinter {
    enum TypeHint {
        OBJECT,
        MAP_LIKE,
        LIST_LIKE,
        SCALAR;

        public boolean matches(@NotNull ConfigElement element) {
            return switch (this) {
                case OBJECT, MAP_LIKE -> element.isNode();
                case LIST_LIKE -> element.isList();
                case SCALAR -> element.isScalar();
            };
        }
    }

    @NotNull TypeHint getHint(@NotNull Type type);
}
