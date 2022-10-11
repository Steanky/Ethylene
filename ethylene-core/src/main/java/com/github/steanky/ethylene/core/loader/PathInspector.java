package com.github.steanky.ethylene.core.loader;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * An object which can extract information about a specific path.
 */
public interface PathInspector {
    /**
     * Gets the extension string from a path. For example, given the Unix path {@code /home/user/test.txt}, this method
     * should return {@code txt}. Files without extensions will cause an empty string to be returned. The path does not
     * have to exist in order for this method to return the expected value.
     *
     * @param path the path from which to extract an extension
     * @return the file extension
     */
    @NotNull String getExtension(@NotNull Path path);

    /**
     * Gets the file name string from a path. For example, given the Unix path {@code /home/user/test.txt}, this method
     * should return {@code test}. The path does not have to exist in order for this method to return the expected
     * value.
     *
     * @param path the path from which to extract a filename
     * @return the filename
     */
    @NotNull String getName(@NotNull Path path);
}
