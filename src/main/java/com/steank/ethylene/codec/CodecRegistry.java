package com.steank.ethylene.codec;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class CodecRegistry {
    public static final CodecRegistry INSTANCE = new CodecRegistry();

    private final Map<String, ConfigCodec> codecMap = new HashMap<>();

    //enforce singleton
    private CodecRegistry() {}

    public void registerCodec(@NotNull ConfigCodec codec) {
        Objects.requireNonNull(codec);

        Set<String> names = codec.getNames();
        if(names.isEmpty()) {
            throw new IllegalArgumentException("codecs must provide at least one name");
        }

        for(String name : codec.getNames()) {
            String lowerCase = Objects.requireNonNull(name, "codecs cannot have any null names")
                    .toLowerCase(Locale.ROOT);

            if(codecMap.containsKey(lowerCase)) {
                throw new IllegalArgumentException("a codec for name " + lowerCase + " has already been registered");
            }
            else {
                codecMap.put(lowerCase, codec);
            }
        }
    }

    public ConfigCodec getCodec(@NotNull String name) {
        return codecMap.get(Objects.requireNonNull(name));
    }

    public boolean hasCodec(@NotNull String name) {
        return codecMap.containsKey(Objects.requireNonNull(name));
    }
}
