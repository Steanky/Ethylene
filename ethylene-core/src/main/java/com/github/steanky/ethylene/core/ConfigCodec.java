package com.github.steanky.ethylene.core;

import com.github.steanky.ethylene.core.collection.ConfigNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * <p>This interface represents the primary compatibility layer between any specific configuration format and a
 * {@link ConfigNode} object. Codec implementations represent specific data formats (e.g. TOML, YAML, ...).</p>
 * <p>
 * As a general rule of thumb, ConfigCodec implementations should be immutable.
 */
public interface ConfigCodec {
    /**
     * <i>Encodes</i> a provided {@link ConfigElement} (writes it to an {@link OutputStream}). The format it is written
     * in is dependent on the implementation.
     *
     * @param element the element to write
     * @param output  the OutputStream to write to
     * @throws IOException          if an IO error occurred
     * @throws NullPointerException if any of the arguments are null
     */
    void encode(@NotNull ConfigElement element, @NotNull OutputStream output) throws IOException;

    /**
     * <i>Decodes</i> a {@link ConfigElement} object (reads it from an {@link InputStream}). The format used to
     * interpret the data is implementation dependent.
     *
     * @param input the InputStream to read from
     * @return a ConfigNode object containing the data
     * @throws IOException          if an IO error occurred
     * @throws NullPointerException if any of the arguments are null
     */
    @NotNull ConfigElement decode(@NotNull InputStream input) throws IOException;

    /**
     * <p>Returns a set of strings representing the preferred file extensions for this codec. The set may be empty,
     * in which case no extension should be preferred. Codecs may report any number of extensions. Users of this codec
     * are not required to respect its preferred extensions when reading or writing to files.</p>
     *
     * <p>Any reported string should be filesystem-agnostic.</p>
     *
     * @return a list of preferred extensions for this codec, without a leading period
     */
    @Unmodifiable @NotNull Set<String> getPreferredExtensions();

    /**
     * Returns a non-null string representing the single preferred extension that should (but is not required to) be
     * used when saving configuration data to a file using this codec. If this codec does not report any preferred
     * extensions, the returned string must be empty. In general, if non-empty, the string reported by this method
     * should be contained in the set of strings returned by {@link ConfigCodec#getPreferredExtensions()}.
     *
     * @return the preferred extension, without any leading period, or an empty string
     */
    @NotNull String getPreferredExtension();
}
