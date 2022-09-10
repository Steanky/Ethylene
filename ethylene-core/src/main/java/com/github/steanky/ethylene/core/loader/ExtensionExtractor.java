package com.github.steanky.ethylene.core.loader;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public interface ExtensionExtractor {
    @NotNull String getExtension(@NotNull Path path);

    @NotNull String getName(@NotNull Path path);

    boolean hasExtension(@NotNull Path path);
}
