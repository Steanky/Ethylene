package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.codec.ConfigCodec;
import com.github.steanky.ethylene.core.util.FutureUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Synchronous specialization of {@link FileConfigLoader}.
 * @param <TData> the type of data object
 */
public class SyncFileConfigLoader<TData> extends FileConfigLoader<TData> {
    /**
     * Constructs a new SyncFileConfigLoader instance from the given {@link ConfigProcessor}, data object, {@link Path}
     * and {@link ConfigCodec}.
     * @param processor the processor used to marshal data
     * @param defaultData the default data object
     * @param path the path to read data from and write defaults to
     * @param codec the {@link ConfigCodec} used to decode the file data
     */
    public SyncFileConfigLoader(@NotNull ConfigProcessor<TData> processor,
                                @NotNull TData defaultData,
                                @NotNull Path path,
                                @NotNull ConfigCodec codec) {
        super(processor, defaultData, new FileCodecConfigBridge(path, codec) {
            @Override
            protected @NotNull <TReturn> CompletableFuture<TReturn> makeFuture(@NotNull Callable<TReturn> callable) {
                return FutureUtils.completeCallableSync(callable);
            }
        }, path);
    }
}
