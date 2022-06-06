package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.codec.ConfigCodec;
import com.github.steanky.ethylene.core.util.FutureUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Asynchronous specialization of {@link FileConfigLoader}.
 * @param <TData> the type of data object
 */
public class AsyncFileConfigLoader<TData> extends FileConfigLoader<TData> {
    /**
     * Constructs a new AsyncFileConfigLoader instance from the given {@link ConfigProcessor}, data object,
     * {@link Path}, {@link ConfigCodec}, and {@link Executor}.
     * @param processor the processor used to marshal data
     * @param defaultData the default data object
     * @param path the path to read data from and write defaults to
     * @param codec the {@link ConfigCodec} used to decode the file data
     * @param executor the executor used to perform read and write operations asynchronously
     */
    public AsyncFileConfigLoader(@NotNull ConfigProcessor<TData> processor,
                                 @NotNull TData defaultData,
                                 @NotNull Path path,
                                 @NotNull ConfigCodec codec,
                                 @NotNull Executor executor) {
        super(processor, defaultData, new FileCodecConfigBridge(path, codec) {
            @Override
            protected @NotNull <TReturn> CompletableFuture<TReturn> makeFuture(@NotNull Callable<TReturn> callable) {
                return FutureUtils.completeCallableAsync(callable, executor);
            }
        }, path);

        Objects.requireNonNull(executor);
    }
}
