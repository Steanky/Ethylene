package com.github.steank.ethylene.codec;

import com.github.steank.ethylene.collection.ConfigNode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.function.Supplier;

/**
 * This interface represents the primary compatibility layer between any specific configuration format and a
 * {@link ConfigNode} object.
 */
public interface ConfigCodec {
    /**
     * <i>Encodes</i> a provided {@link ConfigNode} (writes it to an {@link OutputStream}). The format it is written is
     * dependent on the implementation.
     * @param node the node to write
     * @param output the OutputStream to write to
     * @param close if the OutputStream should be closed after writing
     * @throws IOException if an IO error occurred
     * @throws NullPointerException if any of the arguments are null
     */
    void encodeNode(@NotNull ConfigNode node, @NotNull OutputStream output, boolean close) throws IOException;

    /**
     * <i>Decodes</i> a {@link ConfigNode} object (reads it from an {@link InputStream}). The format used it interpret
     * the data is implementation dependent.
     * @param input the InputStream to read from
     * @param close if the InputStream should be closed after reading
     * @param nodeSupplier the {@link Supplier} used to construct the returned node
     * @param <TNode> the type of ConfigNode that is returned
     * @return a ConfigNode object containing the data
     * @throws IOException if an IO error occurred
     * @throws NullPointerException if any of the arguments are null
     */
    <TNode extends ConfigNode> @NotNull TNode decodeNode(@NotNull InputStream input, boolean close,
                                                         @NotNull Supplier<TNode> nodeSupplier) throws IOException;

    /**
     * Provides a set of strings used to identify this codec. It should be non-empty and contain no null elements. It is
     * acceptable in most cases for codecs to only provide a single name. For example, a ConfigCodec for JSON format
     * might simply return <b>Set.of("json")</b>.
     * @return a set of strings which are used to identify this codec
     */
    @NotNull Set<String> getNames();
}
