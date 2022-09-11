package com.github.steanky.ethylene.codec.toml.mapper;

import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.BasicTypeHinter;
import com.github.steanky.ethylene.mapper.TypeHinter;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Date;

public class TomlTypeHinter extends BasicTypeHinter {
    public static final TypeHinter INSTANCE = new TomlTypeHinter();

    @Override
    public @NotNull ElementType getHint(@NotNull Type type) {
        if (TypeUtils.isAssignable(type, Date.class)) {
            return ElementType.SCALAR;
        }

        return super.getHint(type);
    }
}
