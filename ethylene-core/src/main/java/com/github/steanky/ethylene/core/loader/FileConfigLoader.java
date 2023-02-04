package com.github.steanky.ethylene.core.loader;

import com.github.steanky.ethylene.core.ConfigCodec;
import com.github.steanky.ethylene.core.bridge.ConfigSource;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * <p>Filesystem specialization of ProcessingConfigLoader. The in is considered <i>absent</i> if the given
 * {@link Path} does not exist (see {@link ProcessingConfigLoader} for more information on how an absent in changes
 * behavior).</p>
 *
 * @param <TData> the type of data object
 */
public class FileConfigLoader<TData> extends ProcessingConfigLoader<TData> {
    private final Path path;

    /**
     * Constructs a new FileConfigLoader instance from the given {@link ConfigProcessor}, data object, {@link Path}, and
     * {@link ConfigCodec}.
     *
     * @param processor   the processor used to marshal data
     * @param defaultData the default data object
     * @param path        the path to read data from and write defaults to
     * @param bridge      the ConfigSource used to read/write data
     * @throws IllegalArgumentException if path represents a directory
     */
    FileConfigLoader(@NotNull ConfigProcessor<TData> processor, @NotNull TData defaultData,
        @NotNull ConfigSource bridge, @NotNull Path path) {
        super(processor, defaultData, bridge);
        this.path = Objects.requireNonNull(path);
    }

    @Override
    protected boolean isAbsent() {
        return Files.notExists(path);
    }
}