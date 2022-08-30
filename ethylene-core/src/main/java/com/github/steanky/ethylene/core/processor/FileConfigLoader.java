package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.bridge.ConfigBridge;
import com.github.steanky.ethylene.core.codec.ConfigCodec;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * <p>Filesystem-in specialization of ProcessingConfigLoader. The in is considered <i>absent</i> if the given
 * {@link Path} does not exist (see {@link ProcessingConfigLoader} for more information on how an absent in changes
 * behavior).</p>
 *
 * <p>This class is only instantiated internally because there is some potential for misuse; i.e. it is possible for a
 * user to provide a {@link ConfigBridge} implementation that is not at all related to the given Path.</p>
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
     * @param bridge      the ConfigBridge used to read/write data
     * @throws IllegalArgumentException if path represents a directory
     */
    FileConfigLoader(@NotNull ConfigProcessor<TData> processor, @NotNull TData defaultData,
            @NotNull ConfigBridge bridge, @NotNull Path path) {
        super(processor, defaultData, bridge);
        this.path = Objects.requireNonNull(path);
    }

    @Override
    protected boolean isAbsent() {
        return Files.notExists(path);
    }
}