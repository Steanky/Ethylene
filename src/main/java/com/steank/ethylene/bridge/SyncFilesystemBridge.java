package com.steank.ethylene.bridge;

import com.steank.ethylene.collection.FileConfigNode;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * A synchronous implementation of {@link AbstractFilesystemBridge}.
 */
public class SyncFilesystemBridge extends AbstractFilesystemBridge {
    public SyncFilesystemBridge(@NotNull Path root) {
        super(root);
    }

    @Override
    protected Future<FileConfigNode> callRead(@NotNull Callable<FileConfigNode> callable) throws Exception {
        return CompletableFuture.completedFuture(callable.call());
    }

    @Override
    protected Future<Void> callWrite(@NotNull Callable<Void> callable) throws Exception {
        return CompletableFuture.completedFuture(callable.call());
    }
}
