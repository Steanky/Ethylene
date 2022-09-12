package com.github.steanky.ethylene.core.loader;

import com.github.steanky.ethylene.core.ConfigCodec;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.bridge.ConfigSource;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Internal {@link ConfigSource} implementation used by {@link AsyncFileConfigLoader} and {@link SyncFileConfigLoader}.
 */
abstract class FileCodecConfigSource implements ConfigSource {
    private final Path path;
    private final ConfigCodec codec;

    /**
     * Constructs a new FileCodecConfigSource from the provided {@link Path} and {@link ConfigCodec}.
     *
     * @param path  the path to use
     * @param codec the codec to use
     */
    FileCodecConfigSource(@NotNull Path path, @NotNull ConfigCodec codec) {
        this.path = Objects.requireNonNull(path);
        this.codec = Objects.requireNonNull(codec);
    }

    @Override
    public @NotNull CompletableFuture<ConfigElement> read() {
        return makeFuture(() -> codec.decode(Files.newInputStream(path)));
    }

    @Override
    public @NotNull CompletableFuture<Void> write(@NotNull ConfigElement element) {
        return makeFuture(() -> {
            codec.encode(element, Files.newOutputStream(path));
            return null;
        });
    }

    /**
     * Produces a {@link CompletableFuture} object from a {@link Callable}. The future may or may not be already
     * completed, depending on if the implementation is synchronous or asynchronous.
     *
     * @param callable  the callable to convert
     * @param <TReturn> the type of data object
     * @return a {@link CompletableFuture} object which may represent a synchronous or asynchronous call
     */
    protected abstract <TReturn> @NotNull CompletableFuture<TReturn> makeFuture(@NotNull Callable<TReturn> callable);
}
