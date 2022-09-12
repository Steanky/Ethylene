package com.github.steanky.ethylene.core.loader;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class BasicPathNameInspector implements PathNameInspector {
    private static final char EXTENSION_SEPARATOR = '.';
    private static final String EMPTY_STRING = "";

    @Override
    public @NotNull String getExtension(@NotNull Path path) {
        Path namePath = path.getFileName();
        if (namePath == null) {
            return EMPTY_STRING;
        }

        String namePathString = namePath.toString();
        int extensionIndex = namePathString.lastIndexOf(EXTENSION_SEPARATOR);
        if (extensionIndex == -1) {
            return EMPTY_STRING;
        }

        return namePathString.substring(extensionIndex + 1);
    }

    @Override
    public @NotNull String getName(@NotNull Path path) {
        Path namePath = path.getFileName();
        if (namePath == null) {
            return EMPTY_STRING;
        }

        String namePathString = namePath.toString();
        int extensionIndex = namePathString.lastIndexOf(EXTENSION_SEPARATOR);
        if (extensionIndex == -1) {
            return namePathString;
        }

        return namePathString.substring(0, extensionIndex);
    }

    @Override
    public boolean hasExtension(@NotNull Path path) {
        return !getExtension(path).isEmpty();
    }
}
