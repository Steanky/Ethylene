package com.github.steanky.ethylene.codec.toml.mapper;

import com.github.steanky.ethylene.codec.toml.ConfigDate;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.mapper.BasicScalarSource;
import com.github.steanky.ethylene.mapper.ScalarSource;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;

/**
 * Special {@link ScalarSource} implementation which supports TOML date primitives. This is only meant to be used in
 * conjunction with the {@code ethylene-mapper} module.
 */
public class TomlScalarSource extends BasicScalarSource {
    /**
     * The singleton instance of {@link TomlScalarSource}.
     */
    public static final ScalarSource INSTANCE = new TomlScalarSource();

    private TomlScalarSource() {
    }

    @Override
    public @NotNull ConfigElement make(@NotNull Object data) {
        if (data instanceof Temporal temporal) {
            return new ConfigDate(temporal);
        }

        return super.make(data);
    }
}
