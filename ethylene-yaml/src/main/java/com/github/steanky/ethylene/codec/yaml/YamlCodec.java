package com.github.steanky.ethylene.codec.yaml;

import com.github.steanky.ethylene.core.codec.AbstractConfigCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.snakeyaml.engine.v2.api.*;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Provides support for the YAML format.
 */
public class YamlCodec extends AbstractConfigCodec {
    private static final List<String> EXTENSIONS = List.of("yaml", "yml");

    private final Supplier<Load> loadSupplier;
    private final Supplier<Dump> dumpSupplier;

    /**
     * <p>Creates a new YamlCodec that will use the given {@link Supplier} objects to produce {@link Load} and
     * {@link Dump}
     * objects (used to read and write YAML, respectively).</p>
     *
     * <p>Note that users should ensure the suppliers always return new objects, as each Load and Dump instance may
     * only be used to read or write once.</p>
     *
     * @param loadSupplier the supplier creating Load instances
     * @param dumpSupplier the supplier created Dump instances
     */
    public YamlCodec(@NotNull Supplier<Load> loadSupplier, @NotNull Supplier<Dump> dumpSupplier) {
        this.loadSupplier = Objects.requireNonNull(loadSupplier);
        this.dumpSupplier = Objects.requireNonNull(dumpSupplier);
    }

    /**
     * Creates a new YamlCodec using a default loadSupplier and dumpSupplier.
     */
    public YamlCodec() {
        this(() -> new Load(LoadSettings.builder().build()), () -> new Dump(DumpSettings.builder().build()));
    }

    @Override
    protected @NotNull Object readObject(@NotNull InputStream input) throws IOException {
        try {
            Iterable<Object> objectIterable = loadSupplier.get().loadAllFromInputStream(input);
            List<Object> objectList = new ArrayList<>();
            for (Object object : objectIterable) {
                objectList.add(object);
            }

            //support loading multiple YAML documents from one stream
            if (objectList.size() == 1) {
                return objectList.get(0);
            } else {
                return objectList;
            }
        } catch (YamlEngineException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    protected void writeObject(@NotNull Object object, @NotNull OutputStream output) throws IOException {
        try {
            StreamDataWriter writer = new YamlOutputStreamWriter(output, Charset.defaultCharset()) {
                @Override
                public void processIOException(IOException e) {}
            };

            dumpSupplier.get().dump(object, writer);
            writer.flush();
        } catch (YamlEngineException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    public @Unmodifiable @NotNull List<String> getPreferredExtensions() {
        return EXTENSIONS;
    }
}
