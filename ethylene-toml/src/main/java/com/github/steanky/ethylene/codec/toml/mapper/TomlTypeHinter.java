package com.github.steanky.ethylene.codec.toml.mapper;

import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.BasicTypeHinter;
import com.github.steanky.ethylene.mapper.TypeHinter;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;

/**
 * Special {@link TypeHinter} implementation which supports TOML date primitives. This is only meant to be used in
 * conjunction with the {@code ethylene-mapper} module.
 */
public class TomlTypeHinter extends BasicTypeHinter {
    /**
     * The singleton instance of {@link TomlTypeHinter}.
     */
    public static final TypeHinter INSTANCE = new TomlTypeHinter();

    private TomlTypeHinter() {}

    @Override
    public @NotNull ElementType getHint(@NotNull Token<?> type) {
        if (type.isSubclassOf(Temporal.class)) {
            return ElementType.SCALAR;
        }

        return super.getHint(type);
    }
}
