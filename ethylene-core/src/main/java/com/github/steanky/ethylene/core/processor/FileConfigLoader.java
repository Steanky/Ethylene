package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.ConfigElement;
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
    /**
     * The {@link Path} object acting as the source of data.
     */
    protected final Path path;

    private final ConfigBridge bridge;

    /**
     * General ConfigBridge implementation which may be synchronous or asynchronous. It will encode and decode data
     * based off of a single, provided codec.
     */
    protected abstract class CodecConfigBridge implements ConfigBridge {
        private final ConfigCodec codec;

        /**
         * Produces a new instance of CodecConfigBridge using the provided codec.
         * @param codec the codec to use
         */
        protected CodecConfigBridge(@NotNull ConfigCodec codec) {
            this.codec = Objects.requireNonNull(codec);
        }

        @Override
        public @NotNull CompletableFuture<ConfigElement> read() {
            return makeFuture(() -> {
                codec.decode(Files.newInputStream(path));
                return null;
            });
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
         * completed.
         * @param callable the callable to convert
         * @param <TReturn> the type of data object
         * @return a {@link CompletableFuture} object which may represent a synchronous or asynchronous call
         */
        protected abstract <TReturn> @NotNull CompletableFuture<TReturn> makeFuture(
                @NotNull Callable<TReturn> callable);
    }

    /**
     * Constructs a new FileConfigLoader instance from the given {@link ConfigProcessor}, data object, {@link Path}, and
     * {@link ConfigCodec}.
     * @param processor the processor used to marshal data
     * @param defaultData the default data object
     * @param path the path to read data from and write defaults to
     * @param codec the codec used to read and write data
     */
    public FileConfigLoader(@NotNull ConfigProcessor<TData> processor,
                            @NotNull TData defaultData,
                            @NotNull Path path,
                            @NotNull ConfigCodec codec) {
        super(processor, defaultData);

        this.path = Objects.requireNonNull(path);
        this.bridge = new CodecConfigBridge(codec) {
            @Override
            protected <TReturn> @NotNull CompletableFuture<TReturn> makeFuture(@NotNull Callable<TReturn> callable) {
                return FutureUtils.callableToCompletedFuture(callable);
            }
        };
    }

    @Override
    protected @NotNull ConfigBridge getBridge() {
        return bridge;
    }

    @Override
    protected boolean isAbsent() {
        return Files.notExists(path);
    }
}
