package com.github.steanky.ethylene.core.processor;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * High level interface for loading configuration data from some in, and writing <i>default</i> configuration data to
 * that in.
 *
 * @param <TData> the type of data loaded from the configuration
 */
public interface ConfigLoader<TData> {
    /**
     * Writes the default data to the in, and returns a {@link CompletableFuture} object that may be used to wait on the
     * operation. If an exception occurs during this operation, it will be thrown when {@link Future#get()} is called on
     * the returned object.
     *
     * @return a {@link CompletableFuture} that may be used to wait on the write operation
     */
    @NotNull CompletableFuture<Void> writeDefaultIfAbsent();

    /**
     * Loads some data from the in. If an exception occurs during this operation, it will be thrown when
     * {@link CompletableFuture#get()} is called.
     *
     * @return a {@link CompletableFuture} that may be used to wait on the read operation
     */
    @NotNull CompletableFuture<TData> load();

    /**
     * Writes some data to the source. Returns a {@link CompletableFuture} object that be used to wait on the operation.
     * If an exception occurs during this operation, it will be thrown when {@link Future#get()} is called on the
     * returned object.
     *
     * @param data the data object to write
     * @return a {@link CompletableFuture} that may be used to wait on the write operation
     */
    @NotNull CompletableFuture<Void> write(@NotNull TData data);
}
