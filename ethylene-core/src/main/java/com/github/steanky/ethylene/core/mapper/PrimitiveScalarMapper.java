package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public final class PrimitiveScalarMapper implements ScalarMapper {
    private static final ScalarMapper INSTANCE = new PrimitiveScalarMapper();

    private PrimitiveScalarMapper() {}

    @Override
    public @NotNull Result convertScalar(@NotNull Type type, @NotNull ConfigElement element) {
        if(element.isScalar()) {
            Object object = element.asScalar();
            Class<?> resolved = ReflectionUtils.getUnderlyingClass(type);

            if(resolved.isAssignableFrom(object.getClass())) {
                return new Result(true, object);
            }
        }

        return Result.FAIL;
    }

    public static @NotNull ScalarMapper getInstance() {
        return INSTANCE;
    }
}
