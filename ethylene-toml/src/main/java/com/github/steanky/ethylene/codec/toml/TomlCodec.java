package com.github.steanky.ethylene.codec.toml;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigFormatException;
import com.github.steanky.ethylene.core.codec.AbstractConfigCodec;
import com.google.gson.Gson;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Provides support for the TOML format. This class overrides {@link AbstractConfigCodec#toElement(Object)} and
 * {@link AbstractConfigCodec#toObject(ConfigElement)} in order to provide proper support for dates.
 */
public class TomlCodec extends AbstractConfigCodec {
    /**
     * The default {@link TomlWriter} instance used to read and write data.
     */
    public static final TomlWriter DEFAULT_TOML_WRITER = new TomlWriter();

    private final TomlWriter writer;

    /**
     * Creates a new TomlCodec using the provided {@link TomlWriter} to read and write data.
     * @param writer the TomlWriter instance to use
     * @throws NullPointerException if writer is null
     */
    public TomlCodec(@NotNull TomlWriter writer) {
        this.writer = Objects.requireNonNull(writer);
    }

    /**
     * Creates a new TomlCodec using the default {@link TomlWriter} ({@link TomlCodec#DEFAULT_TOML_WRITER}) to read and
     * write data.
     */
    public TomlCodec() {
        this(DEFAULT_TOML_WRITER);
    }

    @Override
    protected @NotNull Map<String, Object> readMap(@NotNull InputStream input) throws IOException {
        try {
            return new Toml().read(input).toMap();
        }
        catch (IllegalStateException exception) {
            throw new ConfigFormatException(exception);
        }
    }

    @Override
    protected void writeMap(@NotNull Map<String, Object> mappings, @NotNull OutputStream output) throws IOException {
        try {
            writer.write(mappings, output);
        }
        catch (IllegalArgumentException exception) {
            throw new ConfigFormatException(exception);
        }
    }

    @Override
    protected @NotNull ConfigElement toElement(@Nullable Object raw) {
        if(raw instanceof Date date) {
            return new ConfigDate(date);
        }

        return super.toElement(raw);
    }

    @Override
    protected @Nullable Object toObject(@NotNull ConfigElement raw) {
        if(raw instanceof ConfigDate configDate) {
            return configDate.getDate();
        }

        return super.toObject(raw);
    }
}
