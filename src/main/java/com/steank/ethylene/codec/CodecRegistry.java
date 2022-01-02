package com.steank.ethylene.codec;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Used to store codecs and associate them with particular names. This class is a singleton, whose instance can be
 * accessed through {@link CodecRegistry#INSTANCE}.
 */
public final class CodecRegistry {
    /**
     * The CodecRegistry instance.
     */
    public static final CodecRegistry INSTANCE = new CodecRegistry();

    private final Map<String, ConfigCodec> codecMap = new HashMap<>();

    //enforce singleton
    private CodecRegistry() {}

    /**
     * Registers a {@link ConfigCodec} with this registry. The codec will be associated with each of the names it
     * provides. Names are treated as case-insensitive.
     * @param codec the codec to register
     * @throws IllegalArgumentException if the codec does not supply any names, or if another codec has already been
     * registered under one of the names it supplies
     * @throws NullPointerException if the codec supplies a null name
     */
    public void registerCodec(@NotNull ConfigCodec codec) {
        Objects.requireNonNull(codec);

        Set<String> names = codec.getNames();
        if(names.isEmpty()) {
            throw new IllegalArgumentException("Codecs must provide at least one name");
        }

        for(String name : codec.getNames()) {
            String lowerCase = Objects.requireNonNull(name, "Codecs cannot have any null names")
                    .toLowerCase(Locale.ROOT);

            if(codecMap.containsKey(lowerCase)) {
                throw new IllegalArgumentException("A codec for name " + lowerCase + " has already been registered");
            }
            else {
                codecMap.put(lowerCase, codec);
            }
        }
    }

    /**
     * Obtains a {@link ConfigCodec} for the specified name. Names are case-insensitive.
     * @param name the name of the codec
     * @return The ConfigCodec associated with name, or null if none exist
     * @throws NullPointerException if name is null
     */
    public ConfigCodec getCodec(@NotNull String name) {
        return codecMap.get(Objects.requireNonNull(name).toLowerCase(Locale.ROOT));
    }

    /**
     * Can be used to determine if a {@link ConfigCodec} has been registered under a specific name, which is
     * case-insensitive.
     * @param name the name to check for
     * @return true if a ConfigCodec has been registered for name, false otherwise
     * @throws NullPointerException if name is null
     */
    public boolean hasCodec(@NotNull String name) {
        return codecMap.containsKey(Objects.requireNonNull(name).toLowerCase(Locale.ROOT));
    }
}
