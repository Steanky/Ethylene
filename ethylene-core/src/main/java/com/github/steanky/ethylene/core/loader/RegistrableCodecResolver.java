package com.github.steanky.ethylene.core.loader;

import com.github.steanky.ethylene.core.ConfigCodec;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class RegistrableCodecResolver implements CodecResolver {
    private final Map<String, ConfigCodec> codecMap;

    public RegistrableCodecResolver() {
        this.codecMap = new HashMap<>(4);
    }

    @Override
    public @NotNull ConfigCodec resolve(@NotNull String name) {
        ConfigCodec codec = codecMap.get(name);
        if (codec == null) {
            throw new IllegalArgumentException("unregistered codec named " + name);
        }

        return codec;
    }

    @Override
    public boolean hasCodec(@NotNull String name) {
        return codecMap.containsKey(name);
    }

    public void registerCodec(@NotNull ConfigCodec codec) {
        for (String name : codec.getPreferredExtensions()) {
            codecMap.put(name, codec);
        }
    }
}
