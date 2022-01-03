package com.github.steanky.ethylene.codec;

import org.jetbrains.annotations.NotNull;

/**
 * Used to store supported codecs and associate them with particular names.
 */
public interface CodecRegistry {
    /**
     * Registers a {@link ConfigCodec} with this registry. The codec will be associated with each of the names it
     * provides. Names are treated as case-insensitive.
     * @param codec the codec to register
     * @throws IllegalArgumentException if the codec does not supply any names, or if another codec has already been
     * registered under one of the names it supplies
     * @throws NullPointerException if the codec supplies any null names
     */
    void registerCodec(@NotNull ConfigCodec codec);

    /**
     * Obtains a {@link ConfigCodec} for the specified name. Names are case-insensitive.
     * @param name the name of the codec
     * @return The ConfigCodec associated with name, or null if none exist
     * @throws NullPointerException if name is null
     */
    ConfigCodec getCodec(@NotNull String name);

    /**
     * Can be used to determine if a {@link ConfigCodec} has been registered under a specific name, which is
     * case-insensitive.
     * @param name the name to check for
     * @return true if a ConfigCodec has been registered for name, false otherwise
     * @throws NullPointerException if name is null
     */
    boolean hasCodec(@NotNull String name);
}
