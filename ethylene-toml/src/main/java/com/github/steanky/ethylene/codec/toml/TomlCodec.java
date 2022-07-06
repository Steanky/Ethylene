package com.github.steanky.ethylene.codec.toml;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.codec.AbstractConfigCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Provides support for the TOML format. This class overrides {@link AbstractConfigCodec#serializeElement(ConfigElement)} and
 * {@link AbstractConfigCodec#deserializeObject(Object)} in order to provide proper support for dates.
 */
public class TomlCodec extends AbstractConfigCodec {
    private static final List<String> EXTENSIONS = List.of("toml");

    private final TomlParser parser;
    private final TomlWriter writer;

    /**
     * Creates a new TomlCodec with default values.
     */
    public TomlCodec() {
        this.parser = new TomlParser();
        this.writer = new TomlWriter();
    }

    /**
     * Creates a new TomlCodec using the given {@link TomlParser} and {@link TomlWriter}.
     * @param parser the TomlParser to use to read TOML
     * @param writer the TomlWriter used to write TOML
     */
    public TomlCodec(@NotNull TomlParser parser, @NotNull TomlWriter writer) {
        this.parser = Objects.requireNonNull(parser);
        this.writer = Objects.requireNonNull(writer);
    }

    @Override
    protected @NotNull Object readObject(@NotNull InputStream input) throws IOException {
        try {
            return parser.parse(input);
        }
        catch (ParsingException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void writeObject(@NotNull Object object, @NotNull OutputStream output) throws IOException {
        try {
            writer.write((UnmodifiableConfig) object, output);
        }
        catch (WritingException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected @Nullable Object serializeElement(@NotNull ConfigElement element) {
        if(element instanceof ConfigDate configDate) {
            return configDate.getDate();
        }

        return super.serializeElement(element);
    }

    @Override
    protected @NotNull ConfigElement deserializeObject(@Nullable Object object) {
        if(object instanceof Date date) {
            return new ConfigDate(date);
        }

        return super.deserializeObject(object);
    }

    @Override
    protected @NotNull <TOut> Output<TOut> makeEncodeMap() {
        Config config = TomlFormat.newConfig(LinkedHashMap::new);
        return new Output<>(config, config::add);
    }

    @Override
    protected boolean isContainer(@Nullable Object input) {
        return super.isContainer(input) || input instanceof UnmodifiableConfig;
    }

    @Override
    protected @NotNull <TOut> Node<TOut> makeNode(@NotNull Object inputContainer,
                                                  @NotNull Supplier<Output<TOut>> mapSupplier,
                                                  @NotNull Supplier<Output<TOut>> collectionSupplier) {
        if(inputContainer instanceof UnmodifiableConfig object) {
            return new Node<>(object, object.entrySet().stream().map(member -> new Entry<>(member.getKey(), member
                    .getValue())).iterator(), mapSupplier.get());
        }
        else {
            return super.makeNode(inputContainer, mapSupplier, collectionSupplier);
        }
    }

    @Override
    public @Unmodifiable @NotNull List<String> getPreferredExtensions() {
        return EXTENSIONS;
    }
}
