package com.github.steanky.ethylene.core.loader;

import com.github.steanky.ethylene.core.ConfigCodec;
import org.jetbrains.annotations.NotNull;

/**
 * A simple store of {@link ConfigCodec} implementations.
 */
public interface CodecResolver {
    /**
     * Resolves the codec, given the provided name.
     * @param name the name
     * @return a {@link ConfigCodec}
     * @throws IllegalArgumentException if no codec exists with the provided name
     */
    @NotNull ConfigCodec resolve(@NotNull String name);

    /**
     * Determines if this resolver has the given codec.
     *
     * @param name the name to check
     * @return true if this resolver has the codec; false otherwise
     */
    boolean hasCodec(@NotNull String name);
}
