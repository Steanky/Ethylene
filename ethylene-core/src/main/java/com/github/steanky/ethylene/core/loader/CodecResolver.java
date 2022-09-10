package com.github.steanky.ethylene.core.loader;

import com.github.steanky.ethylene.core.ConfigCodec;
import org.jetbrains.annotations.NotNull;

public interface CodecResolver {
    @NotNull ConfigCodec resolve(@NotNull String name);

    boolean hasCodec(@NotNull String name);
}
