package com.github.steanky.ethylene.codec.yaml;

import com.github.steanky.ethylene.core.codec.AbstractConfigCodec;
import org.jetbrains.annotations.NotNull;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.StreamDataWriter;
import org.snakeyaml.engine.v2.api.YamlOutputStreamWriter;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Supplier;

public class YamlCodec extends AbstractConfigCodec {
    private final Supplier<Load> loadSupplier;
    private final Supplier<Dump> dumpSupplier;

    public YamlCodec(@NotNull Supplier<Load> loadSupplier,
                     @NotNull Supplier<Dump> dumpSupplier) {
        this.loadSupplier = Objects.requireNonNull(loadSupplier);
        this.dumpSupplier = Objects.requireNonNull(dumpSupplier);
    }

    @Override
    protected @NotNull Object readObject(@NotNull InputStream input) throws IOException {
        try {
            return loadSupplier.get().loadFromInputStream(input);
        }
        catch (YamlEngineException exception) {
            throw new IOException(exception);
        }
    }

    @Override
    protected void writeObject(@NotNull Object object, @NotNull OutputStream output) throws IOException {
        try {
            dumpSupplier.get().dump(object, new YamlOutputStreamWriter(output, Charset.defaultCharset()) {
                @Override
                public void processIOException(IOException e) {}
            });
        }
        catch (YamlEngineException exception) {
            throw new IOException(exception);
        }
    }
}
