package com.steank.ethylene.codec;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
            Objects.requireNonNull(name, "codecs cannot have any null names");

            if(codecMap.containsKey(name)) {
                throw new IllegalArgumentException("a codec for name " + name + " has already been registered");
            }
            else {
                codecMap.put(name, codec);
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
