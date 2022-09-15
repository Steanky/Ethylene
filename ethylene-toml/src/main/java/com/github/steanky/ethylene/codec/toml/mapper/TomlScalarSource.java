package com.github.steanky.ethylene.codec.toml.mapper;

import com.github.steanky.ethylene.codec.toml.ConfigDate;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.bridge.Configuration;
import com.github.steanky.ethylene.mapper.BasicScalarSource;
import com.github.steanky.ethylene.mapper.ScalarSource;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.Temporal;

public class TomlScalarSource extends BasicScalarSource {
    public static final ScalarSource INSTANCE = new TomlScalarSource();

    private TomlScalarSource() {}

    @Override
    public @NotNull ConfigElement make(@NotNull Object data) {
        if (data instanceof Temporal temporal) {
            return new ConfigDate(temporal);
        }

        return super.make(data);
    }
}
