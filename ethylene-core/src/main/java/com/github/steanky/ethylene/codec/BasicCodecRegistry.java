package com.github.steanky.ethylene.codec;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A simple implementation of {@link CodecRegistry}.
 */
public class BasicCodecRegistry implements CodecRegistry {
    private final Map<String, ConfigCodec> codecMap = new HashMap<>();

    public BasicCodecRegistry() {}

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

    public ConfigCodec getCodec(@NotNull String name) {
        return codecMap.get(Objects.requireNonNull(name).toLowerCase(Locale.ROOT));
    }

    public boolean hasCodec(@NotNull String name) {
        return codecMap.containsKey(Objects.requireNonNull(name).toLowerCase(Locale.ROOT));
    }
}
