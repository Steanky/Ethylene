package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.bridge.ConfigBridge;
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
public abstract class AsyncFileConfigLoader<TData> extends FileConfigLoader<TData> {
    private final Executor executor;
    private ConfigBridge bridge;

    /**
     * Constructs a new AsyncFileConfigLoader instance from the given {@link ConfigProcessor}, data object,
     * {@link Path}, and {@link Executor}.
     * @param processor the processor used to marshal data
     * @param defaultData the default data object
     * @param path the path to read data from and write defaults to
     * @param executor the executor used to perform read and write operations asynchronously
     */
    public AsyncFileConfigLoader(@NotNull ConfigProcessor<TData> processor,
                                 @NotNull TData defaultData,
                                 @NotNull Path path,
                                 @NotNull Executor executor) {
        super(processor, defaultData, path);
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    protected @NotNull ConfigBridge getBridge() {
        if(bridge != null) {
            return bridge;
        }

        return bridge = new ValidatingConfigBridge() {
            @Override
            protected @NotNull <TReturn> CompletableFuture<TReturn> makeFuture(@NotNull Callable<TReturn> callable) {
                return FutureUtils.callableToFuture(callable, executor);
            }
        };
    }
}
