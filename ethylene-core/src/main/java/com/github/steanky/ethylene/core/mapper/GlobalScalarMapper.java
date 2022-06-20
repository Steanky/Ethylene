package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public final class GlobalScalarMapper implements ScalarMapper {
    private static final ScalarMapper INSTANCE = new GlobalScalarMapper();

    private GlobalScalarMapper() {}

    @Override
    public @NotNull Object convertScalar(@NotNull Type type, @NotNull ConfigElement element) {
        if(element.isScalar()) {
            Object object = element.asScalar();
            Class<?> resolved = ReflectionUtils.getUnderlyingClass(type);

            if(resolved.isAssignableFrom(object.getClass())) {
                return object;
            }
        }

        throw new MappingException("Unable to convert ConfigElement " + element + " to scalar type " + type
                .getTypeName());
    }

    public static @NotNull ScalarMapper getInstance() {
        return INSTANCE;
    }
}
