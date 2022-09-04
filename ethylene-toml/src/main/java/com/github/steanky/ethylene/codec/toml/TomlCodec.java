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
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.graph.GraphTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Provides support for the TOML format. This class overrides
 * {@link AbstractConfigCodec#serializeElement(ConfigElement)} and {@link AbstractConfigCodec#deserializeObject(Object)}
 * in order to provide proper support for dates.
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
     *
     * @param parser the TomlParser to use to read TOML
     * @param writer the TomlWriter used to write TOML
     */
    public TomlCodec(@NotNull TomlParser parser, @NotNull TomlWriter writer) {
        this.parser = Objects.requireNonNull(parser);
        this.writer = Objects.requireNonNull(writer);
    }

    @Override
    protected @NotNull GraphTransformer.Node<Object, ConfigElement, String> makeDecodeNode(@NotNull Object target) {
        if (target instanceof UnmodifiableConfig config) {
            return new GraphTransformer.Node<>(target, new Iterator<>() {
                private final Iterator<? extends UnmodifiableConfig.Entry> backing = config.entrySet().iterator();

                @Override
                public boolean hasNext() {
                    return backing.hasNext();
                }

                @Override
                public Entry<String, Object> next() {
                    UnmodifiableConfig.Entry next = backing.next();
                    return Entry.of(next.getKey(), next.getValue());
                }
            }, makeDecodeMap(config.size()));
        }

        return super.makeDecodeNode(target);
    }

    @Override
    protected @NotNull GraphTransformer.Output<Object, String> makeEncodeMap(int size) {
        Config config = TomlFormat.newConfig(() -> new LinkedHashMap<>(size));
        return new GraphTransformer.Output<>(config, (k, v, b) -> config.add(k, v));
    }

    @Override
    protected boolean isContainer(@Nullable Object input) {
        return super.isContainer(input) || input instanceof UnmodifiableConfig;
    }

    @Override
    protected @Nullable Object serializeElement(@NotNull ConfigElement element) {
        if (element instanceof ConfigDate configDate) {
            return configDate.getDate();
        }

        return super.serializeElement(element);
    }

    @Override
    protected @NotNull ConfigElement deserializeObject(@Nullable Object object) {
        if (object instanceof Date date) {
            return new ConfigDate(date);
        }

        return super.deserializeObject(object);
    }

    @Override
    protected @NotNull Object readObject(@NotNull InputStream input) throws IOException {
        try {
            return parser.parse(input);
        } catch (ParsingException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void writeObject(@NotNull Object object, @NotNull OutputStream output) throws IOException {
        try {
            writer.write((UnmodifiableConfig) object, output);
        } catch (WritingException e) {
            throw new IOException(e);
        }
    }

    @Override
    public @Unmodifiable @NotNull List<String> getPreferredExtensions() {
        return EXTENSIONS;
    }
}
