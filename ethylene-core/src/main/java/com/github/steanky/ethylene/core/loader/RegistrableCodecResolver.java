package com.github.steanky.ethylene.core.loader;

import com.github.steanky.ethylene.core.ConfigCodec;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RegistrableCodecResolver implements CodecResolver {
    private final Map<String, ConfigCodec> codecMap;

    public RegistrableCodecResolver() {
        this.codecMap = new HashMap<>(4);
    }

    @Override
    public @NotNull ConfigCodec resolve(@NotNull String extension) {
        ConfigCodec codec = codecMap.get(extension);
        if (codec == null) {
            throw new IllegalArgumentException("unregistered codec with extension " + extension);
        }

        return codec;
    }

    @Override
    public boolean hasCodec(@NotNull String extension) {
        return codecMap.containsKey(extension);
    }

    public void registerCodec(@NotNull ConfigCodec codec) {
        Set<String> extensions = codec.getPreferredExtensions();
        if (extensions.isEmpty()) {
            codecMap.put("", codec);
        } else {
            for (String extension : extensions) {
                codecMap.put(extension, codec);
            }
        }
    }
}
