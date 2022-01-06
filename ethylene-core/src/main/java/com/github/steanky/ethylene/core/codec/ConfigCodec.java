package com.github.steanky.ethylene.core.codec;

import com.github.steanky.ethylene.core.collection.ConfigNode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
     * @throws IOException if an IO error occurred
     * @throws NullPointerException if any of the arguments are null
     */
    void encodeNode(@NotNull ConfigNode node, @NotNull OutputStream output) throws IOException;

    /**
     * <i>Decodes</i> a {@link ConfigNode} object (reads it from an {@link InputStream}). The format used to interpret
     * the data is implementation dependent.
     * @param input the InputStream to read from
     * @param nodeSupplier the {@link Supplier} used to construct the returned node
     * @param <TNode> the type of ConfigNode that is returned
     * @return a ConfigNode object containing the data
     * @throws IOException if an IO error occurred
     * @throws NullPointerException if any of the arguments are null
     */
    <TNode extends ConfigNode> @NotNull TNode decodeNode(@NotNull InputStream input,
                                                         @NotNull Supplier<TNode> nodeSupplier) throws IOException;
}
