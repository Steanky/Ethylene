package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class BasicTypeHinter implements TypeHinter {
    @Override
    public @NotNull TypeHint getHint(@NotNull Type type) {
        Class<?> underlying = ReflectionUtils.getUnderlyingClass(type);

        if(List.class.isAssignableFrom(underlying)) {
            return TypeHint.LIST_LIKE;
        }
        else if(Map.class.isAssignableFrom(underlying)) {
            return TypeHint.MAP_LIKE;
        }
        else if(underlying.isPrimitive() || String.class.isAssignableFrom(underlying) || Number.class
                .isAssignableFrom(underlying) || Boolean.class.isAssignableFrom(underlying) || Character.class
                .isAssignableFrom(underlying)) {
            return TypeHint.SCALAR;
        }

        return TypeHint.OBJECT;
    }
}
