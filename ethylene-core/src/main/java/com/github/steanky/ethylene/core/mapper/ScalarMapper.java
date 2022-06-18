package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public interface ScalarMapper {
    record Result(boolean successful, @Nullable Object value) {
        public static final Result FAIL = new Result(false, null);
    }

    Object convertScalar(@NotNull Type type, @NotNull ConfigElement element);
}
