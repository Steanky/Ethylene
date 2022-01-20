package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.bridge.ConfigBridge;
import com.github.steanky.ethylene.core.codec.ConfigCodec;
import com.github.steanky.ethylene.core.util.FutureUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Filesystem-source specialization of ProcessingConfigLoader. The source is considered <i>absent</i> if the given
 * {@link Path} does not exist (see {@link ProcessingConfigLoader} for more information on how an absent source changes
 * behavior).
 * @param <TData> the type of data object
 */
public class FileConfigLoader<TData> extends ProcessingConfigLoader<TData> {
    private final Path path;

    /**
     * Constructs a new FileConfigLoader instance from the given {@link ConfigProcessor}, data object, {@link Path}, and
     * {@link ConfigCodec}.
     * @param processor the processor used to marshal data
     * @param defaultData the default data object
     * @param path the path to read data from and write defaults to
     * @param bridge the ConfigBridge used to read/write data
     */
    public FileConfigLoader(@NotNull ConfigProcessor<TData> processor,
                            @NotNull TData defaultData,
                            @NotNull ConfigBridge bridge,
                            @NotNull Path path) {
        super(processor, defaultData, bridge);

        this.path = Objects.requireNonNull(path);
    }

    @Override
    protected boolean isAbsent() {
        return Files.notExists(path);
    }
}
