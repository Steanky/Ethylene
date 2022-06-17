package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.function.BiFunction;

public interface ScalarMapper extends BiFunction<Type, ConfigElement, ScalarMapper.Result> {
    record Result(boolean successful, @Nullable Object value) {
        public static final Result FAIL = new Result(false, null);
    }

    @NotNull Result convertScalar(@NotNull Type type, @NotNull ConfigElement element);

    @Override
    default @NotNull Result apply(Type type, ConfigElement element) {
        return convertScalar(type, element);
    }

    default @NotNull ScalarMapper chain(@NotNull BiFunction<? super Type, ? super ConfigElement, ? extends Result> function) {
        return (type, element) -> {
            Result result = ScalarMapper.this.convertScalar(type, element);
            if(result.successful) {
                return result;
            }

            return function.apply(type, element);
        };
    }
}
