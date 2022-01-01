package com.steank.ethylene.codec;

import com.steank.ethylene.collection.ConfigNode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * This interface represents the primary compatibility layer between any specific configuration format.
 */
public interface ConfigCodec {
    <TMap extends Map<String, Object>> void encodeNode(@NotNull ConfigNode node, @NotNull OutputStream output,
                                                       boolean close,
                                                       @NotNull Supplier<TMap> mapSupplier) throws IOException;

    <TNode extends ConfigNode> @NotNull TNode decodeNode(@NotNull InputStream input, boolean close,
                                                         @NotNull Supplier<TNode> nodeSupplier) throws IOException;

    @NotNull Set<String> getNames();
}
