package com.github.steanky.ethylene.core.loader;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Basic implementation of {@link PathInspector}.
 */
public class BasicPathInspector implements PathInspector {
    /**
     * The singleton instance. This class is immutable.
     */
    public static final PathInspector INSTANCE = new BasicPathInspector();

    private static final char EXTENSION_SEPARATOR = '.';
    private static final String EMPTY_STRING = "";

    private BasicPathInspector() {

    }

    private static String getFilenameString(Path path) {
        Path namePath = path.getFileName();
        if (namePath == null) {
            return EMPTY_STRING;
        }

        return namePath.toString();
    }

    @Override
    public @NotNull String getExtension(@NotNull Path path) {
        String namePathString = getFilenameString(path);
        int extensionIndex = namePathString.lastIndexOf(EXTENSION_SEPARATOR);
        if (extensionIndex == -1) {
            return EMPTY_STRING;
        }

        return namePathString.substring(extensionIndex + 1);
    }

    @Override
    public @NotNull String getName(@NotNull Path path) {
        String namePathString = getFilenameString(path);
        int extensionIndex = namePathString.lastIndexOf(EXTENSION_SEPARATOR);
        if (extensionIndex == -1) {
            return namePathString;
        }

        return namePathString.substring(0, extensionIndex);
    }
}