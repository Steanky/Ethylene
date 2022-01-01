package com.steank.ethylene.bridge;

import com.steank.ethylene.collection.FileConfigNode;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * An asynchronous implementation of {@link AbstractFilesystemBridge}.
 */
public class AsyncFilesystemBridge extends AbstractFilesystemBridge {
    private final ExecutorService executorService;

    public AsyncFilesystemBridge(@NotNull Path root, @NotNull ExecutorService executorService) {
        super(root);
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