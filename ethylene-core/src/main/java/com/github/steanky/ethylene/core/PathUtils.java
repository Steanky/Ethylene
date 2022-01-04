package com.github.steanky.ethylene.core;

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

    private PathUtils() {
        throw new AssertionError("Why would you try to do this?");
    }

    private static String getFileNameInternal(Path path) {
        Path filePath = path.getFileName();

        if(filePath == null) {
            return EMPTY_STRING;
        }
        else {
            return filePath.toString();
        }
    }

    /**
     * Returns the <i>filename</i> of a {@link Path}. This will be the name of the file or directory furthest from the
     * root, including the file extension if present. If the path has no elements, an empty string will be returned.
     * @param path the path
     * @return the name of the file or directory represented by the path, or an empty string if the path contains no
     * elements
     * @throws NullPointerException if path is null
     */
    public static @NotNull String getFileName(@NotNull Path path) {
        return getFileNameInternal(Objects.requireNonNull(path));
    }

    /**
     * Returns the filename of the file referred to by the given {@link Path}, with the file extension removed if it is
     * present, or an empty string if the path has no elements.
     * @param path the path
     * @return the name of the file or directory represented by the path, without any file extension, or an empty string
     * if the path contains no elements
     * @throws NullPointerException if path is null
     */
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

    /**
     * Returns the file extension of the file referred to by the given {@link Path}, or an empty string if the path does
     * not have an extension.
     * @param path the path
     * @return the file extension of the file, if present, or an empty string if it is not present
     * @throws NullPointerException if path is null
     */
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