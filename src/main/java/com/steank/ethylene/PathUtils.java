package com.steank.ethylene;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Provides some utility methods relating to paths.
 */
public final class PathUtils {
    private static final String EMPTY_STRING = "";
    private static final char EXTENSION_SEPARATOR = '.';
    private static final int NOT_FOUND = -1;

    private static String getFileNameInternal(Path path) {
        Path filePath = path.getFileName();

        if(filePath == null) {
            return EMPTY_STRING;
        }
        else {
            return filePath.toString();
        }
    }

    public static @NotNull String getFileName(@NotNull Path path) {
        return getFileNameInternal(Objects.requireNonNull(path));
    }

    public static @NotNull String getFileNameWithoutExtension(@NotNull Path path) {
        Objects.requireNonNull(path);

        String fileName = getFileNameInternal(path);

        int extensionIndex = fileName.lastIndexOf(EXTENSION_SEPARATOR);
        if(extensionIndex == NOT_FOUND) {
            return fileName;
        }
        else {
            return fileName.substring(0, extensionIndex);
        }
    }

    public static @NotNull String getFileExtension(@NotNull Path path) {
        Objects.requireNonNull(path);

        String fileName = getFileNameInternal(path);
        int extensionIndex = fileName.lastIndexOf(EXTENSION_SEPARATOR);
        if(extensionIndex == NOT_FOUND) {
            return EMPTY_STRING;
        }
        else {
            return fileName.substring(extensionIndex + 1);
        }
    }
}