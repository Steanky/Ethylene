package com.github.steanky.ethylene.core.codec;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigNode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This interface represents the primary compatibility layer between any specific configuration format and a
 * {@link ConfigNode} object. As a general rule of thumb, ConfigCodec implementations should be immutable.
 */
public interface ConfigCodec {
    /**
     * <i>Encodes</i> a provided {@link ConfigElement} (writes it to an {@link OutputStream}). The format it is written
     * in is dependent on the implementation.
     * @param element the element to write
     * @param output the OutputStream to write to
     * @throws IOException if an IO error occurred
     * @throws NullPointerException if any of the arguments are null
     */
    void encode(@NotNull ConfigElement element, @NotNull OutputStream output) throws IOException;

    /**
     * <i>Decodes</i> a {@link ConfigElement} object (reads it from an {@link InputStream}). The format used to interpret
     * the data is implementation dependent.
     * @param input the InputStream to read from
     * @return a ConfigNode object containing the data
     * @throws IOException if an IO error occurred
     * @throws NullPointerException if any of the arguments are null
     */
    @NotNull ConfigElement decode(@NotNull InputStream input) throws IOException;
}
