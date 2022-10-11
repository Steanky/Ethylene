package com.github.steanky.ethylene.core.loader;

import com.github.steanky.ethylene.core.ConfigCodec;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A {@link CodecResolver} which allows registration.
 */
public class RegistrableCodecResolver implements CodecResolver {
    private final Map<String, ConfigCodec> codecMap;

    /**
     * Creates a new instance of this class with no registered codecs.
     */
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

    /**
     * Registers a particular codec with this resolver.
     *
     * @param codec the codec to resolve
     */
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
