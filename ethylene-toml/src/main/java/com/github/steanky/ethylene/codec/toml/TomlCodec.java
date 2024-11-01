package com.github.steanky.ethylene.codec.toml;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.github.steanky.ethylene.core.AbstractConfigCodec;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.Graph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.temporal.Temporal;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;

/**
 * Provides support for the TOML format. This class overrides
 * {@link AbstractConfigCodec#serializeElement(ConfigElement)} and {@link AbstractConfigCodec#deserializeObject(Object)}
 * in order to provide proper support for dates.
 */
public class TomlCodec extends AbstractConfigCodec {
    private static final String NAME = "TOML";
    private static final String PREFERRED_EXTENSION = "toml";
    private static final Set<String> EXTENSIONS = Set.of(PREFERRED_EXTENSION);
    private static final int ENCODE_OPTIONS = Graph.Options.TRACK_REFERENCES;
    private static final int DECODE_OPTIONS = Graph.Options.NONE;
    private final TomlParser parser;
    private final TomlWriter writer;

    /**
     * Creates a new TomlCodec with default values.
     */
    public TomlCodec() {
        super(ENCODE_OPTIONS, DECODE_OPTIONS);
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
        super(ENCODE_OPTIONS, DECODE_OPTIONS);
        this.parser = Objects.requireNonNull(parser);
        this.writer = Objects.requireNonNull(writer);
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
    protected @NotNull Graph.Node<Object, ConfigElement, String> makeDecodeNode(@NotNull Object target) {
        if (target instanceof UnmodifiableConfig config) {
            Graph.InputEntry<String, Object, ConfigElement> inputEntry = INPUT_ENTRY.get();

            return Graph.node(new Iterator<>() {
                private final Iterator<? extends UnmodifiableConfig.Entry> backing = config.entrySet().iterator();

                @Override
                public boolean hasNext() {
                    return backing.hasNext();
                }

                @Override
                public Graph.InputEntry<String, Object, ConfigElement> next() {
                    UnmodifiableConfig.Entry next = backing.next();
                    inputEntry.setKey(next.getKey());
                    inputEntry.setValue(next.getValue());
                    return inputEntry;
                }
            }, makeDecodeMap(config.size()));
        }

        return super.makeDecodeNode(target);
    }

    @Override
    protected @NotNull ConfigElement deserializeObject(@Nullable Object object) {
        if (object instanceof Temporal date) {
            return new ConfigDate(date);
        }

        return super.deserializeObject(object);
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
    protected boolean isContainer(@Nullable Object input) {
        return super.isContainer(input) || input instanceof UnmodifiableConfig;
    }

    @Override
    protected @Nullable Object serializeElement(@NotNull ConfigElement element) {
        if (element instanceof ConfigDate configDate) {
            return configDate.getTemporal();
        }

        return super.serializeElement(element);
    }

    @Override
    protected @NotNull Graph.Output<Object, String> makeEncodeMap(int size) {
        Config config = TomlFormat.newConfig(() -> new LinkedHashMap<>(size));
        return Graph.output(config, (k, v, b) -> config.add(k, v));
    }

    @Override
    public @Unmodifiable @NotNull Set<String> getPreferredExtensions() {
        return EXTENSIONS;
    }

    @Override
    public @NotNull String getPreferredExtension() {
        return PREFERRED_EXTENSION;
    }

    @Override
    public @NotNull String getName() {
        return NAME;
    }
}
