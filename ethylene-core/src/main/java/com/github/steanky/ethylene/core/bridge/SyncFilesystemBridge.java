package com.github.steanky.ethylene.core.bridge;

import com.github.steanky.ethylene.core.codec.CodecRegistry;
import com.github.steanky.ethylene.core.collection.FileConfigNode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * A synchronous implementation of {@link AbstractFilesystemBridge}. Read and write operations will be performed
 * synchronously on the invoking thread.
 */
public class SyncFilesystemBridge extends AbstractFilesystemBridge {
    /**
     * Constructs a new SyncFilesystemBridge based off of the specified {@link CodecRegistry} and root {@link Path}.
     * @param codecRegistry the CodecRegistry
     * @param root the root path
     */
    public SyncFilesystemBridge(@NotNull CodecRegistry codecRegistry, @NotNull Path root) {
        super(codecRegistry, root);
    }

    @Override
    protected Future<FileConfigNode> callRead(@NotNull Callable<FileConfigNode> callable) throws IOException {
        try {
            return CompletableFuture.completedFuture(callable.call());
        }
        catch (Exception exception) {
            throw new IOException(exception);
        }
    }

    @Override
    protected Future<Void> callWrite(@NotNull Callable<Void> callable) throws IOException {
        try {
            return CompletableFuture.completedFuture(callable.call());
        }
        catch (Exception exception) {
            throw new IOException(exception);
        }
    }
}
