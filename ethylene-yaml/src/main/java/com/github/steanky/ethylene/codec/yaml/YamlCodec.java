package com.github.steanky.ethylene.codec.yaml;

import com.github.steanky.ethylene.core.AbstractConfigCodec;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.Graph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.snakeyaml.engine.v2.api.*;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Supplier;

/**
 * Provides support for the YAML format.
 */
public class YamlCodec extends AbstractConfigCodec {
    private static final String NAME = "YAML";
    private static final String PREFERRED_EXTENSION = "yml";
    private static final Set<String> EXTENSIONS = Set.of(PREFERRED_EXTENSION, "yaml");

    private static final int ENCODE_OPTIONS = Graph.Options.TRACK_REFERENCES;

    //for YML, it's possible to construct circular references in config, so enable reference tracking
    private static final int DECODE_OPTIONS = Graph.Options.TRACK_REFERENCES;

    private final Supplier<Load> loadSupplier;
    private final Supplier<Dump> dumpSupplier;

    /**
     * Creates a new YamlCodec using a default loadSupplier and dumpSupplier.
     */
    public YamlCodec() {
        this(() -> new Load(LoadSettings.builder().build()), () -> new Dump(DumpSettings.builder().build()));
    }

    /**
     * <p>Creates a new YamlCodec that will use the given {@link Supplier} objects to produce {@link Load} and
     * {@link Dump} objects (used to read and write YAML, respectively).</p>
     *
     * <p>Note that users should ensure the suppliers always return new objects, as each Load and Dump instance may
     * only be used to read or write once.</p>
     *
     * @param loadSupplier the supplier creating Load instances
     * @param dumpSupplier the supplier created Dump instances
     */
    public YamlCodec(@NotNull Supplier<Load> loadSupplier, @NotNull Supplier<Dump> dumpSupplier) {
        super(ENCODE_OPTIONS, DECODE_OPTIONS);
        this.loadSupplier = Objects.requireNonNull(loadSupplier);
        this.dumpSupplier = Objects.requireNonNull(dumpSupplier);
    }

    @Override
    protected @NotNull Object readObject(@NotNull InputStream input) throws IOException {
        Load load = Objects.requireNonNull(loadSupplier.get(), "loadSupplier value");
        try (input) {
            Iterable<Object> objectIterable = load.loadAllFromInputStream(input);
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
        Dump dump = Objects.requireNonNull(dumpSupplier.get(), "dumpSupplier value");
        try (YamlOutputStreamWriter writer = new YamlOutputStreamWriter(output, Charset.defaultCharset()) {
            @Override
            public void processIOException(IOException e) {
            }
        }) {
            if (object instanceof Iterable<?> objects) {
                dump.dumpAll(objects.iterator(), writer);
            } else {
                dump.dump(object, writer);
            }
        } catch (YamlEngineException exception) {
            throw new IOException(exception);
        }
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

    @Override
    public @NotNull Set<ElementType> supportedTopLevelTypes() {
        //always return a new set, EnumSet static methods produce modifiable collections
        return EnumSet.allOf(ElementType.class);
    }
}
