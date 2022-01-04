package com.github.steanky.ethylene.core.bridge;

import com.github.steanky.ethylene.core.codec.CodecRegistry;
import com.github.steanky.ethylene.core.collection.FileConfigNode;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * An asynchronous implementation of {@link AbstractFilesystemBridge} which uses an {@link ExecutorService} to perform
 * read and write operations asynchronously. This class is not responsible for shutting down the ExecutorService it is
 * provided, and will not attempt to do so.
 */
public class AsyncFilesystemBridge extends AbstractFilesystemBridge {
    private final ExecutorService executorService;

    /**
     * Constructs a new AsyncFilesystemBridge based off of the specified {@link CodecRegistry} and root {@link Path},
     * and using the provided {@link ExecutorService} to perform read and write operations asynchronously.
     * @param codecRegistry the codec registry
     * @param root the root path
     * @param executorService the ExecutorService to use for read and write tasks
     * @throws NullPointerException if any of the arguments are null
     */
    public AsyncFilesystemBridge(@NotNull CodecRegistry codecRegistry, @NotNull Path root,
                                 @NotNull ExecutorService executorService) {
        super(codecRegistry, root);
        this.executorService = Objects.requireNonNull(executorService);
    }

    @Override
    protected Future<FileConfigNode> callRead(@NotNull Callable<FileConfigNode> callable) {
        return executorService.submit(callable);
    }

    @Override
    protected Future<Void> callWrite(@NotNull Callable<Void> callable) {
        return executorService.submit(callable);
    }
}