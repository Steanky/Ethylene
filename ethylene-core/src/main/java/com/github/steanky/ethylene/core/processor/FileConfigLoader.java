package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.bridge.ConfigBridge;
import com.github.steanky.ethylene.core.codec.ConfigCodec;
import com.github.steanky.ethylene.core.util.FutureUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * <p>Filesystem-source specialization of ProcessingConfigLoader. The source is considered <i>absent</i> if the given
 * {@link Path} does not exist (see {@link ProcessingConfigLoader} for more information on how an absent source changes
 * behavior).</p>
 *
 * <p>This class does not require a specific codec. Rather, implementations may choose how the codec is loaded by
 * overriding the {@link FileConfigLoader#codecForPath(Path)} method.</p>
 * @param <TData> the type of data object
 */
public abstract class FileConfigLoader<TData> extends ProcessingConfigLoader<TData> {
    /**
     * The {@link Path} object acting as the source of data.
     */
    protected final Path path;

    private ConfigBridge bridge;

    /**
     * General implementation of {@link ConfigBridge} which calls {@link FileConfigLoader#codecForPath(Path)} to obtain
     * a codec. If this call results in a null value (indicating a codec could not be found), exceptions may be thrown
     * when attempting to retrieve the result of a {@link CompletableFuture} returned by
     * {@link ValidatingConfigBridge#read()} or {@link ValidatingConfigBridge#write(ConfigElement)}.
      */
    protected abstract class ValidatingConfigBridge implements ConfigBridge {
        /**
         * The {@link ConfigCodec} instance used to decode file data, or null if there is no codec associated with the
         * file
         */
        protected final ConfigCodec codec = codecForPath(path);

        @Contract("null, _ -> fail")
        private static void validateCodec(ConfigCodec codec, Path path) throws IOException {
            if(codec == null) {
                throw new IOException("No codec found for path " + path);
            }
        }

        @Override
        public @NotNull CompletableFuture<ConfigElement> read() {
            return makeFuture(() -> {
                validateCodec(codec, path);
                codec.decode(Files.newInputStream(path));
                return null;
            });
        }

        @Override
        public @NotNull CompletableFuture<Void> write(@NotNull ConfigElement element) {
            return makeFuture(() -> {
                validateCodec(codec, path);
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
     * Constructs a new FileConfigLoader instance from the given {@link ConfigProcessor}, data object, and {@link Path}.
     * @param processor the processor used to marshal data
     * @param defaultData the default data object
     * @param path the path to read data from and write defaults to
     */
    public FileConfigLoader(@NotNull ConfigProcessor<TData> processor,
                            @NotNull TData defaultData,
                            @NotNull Path path) {
        super(processor, defaultData);
        this.path = Objects.requireNonNull(path);
    }

    @Override
    protected @NotNull ConfigBridge getBridge() {
        if(bridge != null) {
            return bridge;
        }

        return bridge = new ValidatingConfigBridge() {
            @Override
            protected <TReturn> @NotNull CompletableFuture<TReturn> makeFuture(@NotNull Callable<TReturn> callable) {
                return FutureUtils.callableToCompletedFuture(callable);
            }
        };
    }

    @Override
    protected boolean isAbsent() {
        return Files.notExists(path);
    }

    /**
     * Produces a {@link ConfigCodec} object which should be used to read from the given {@link Path}. If no valid
     * codec can be found, this function should return null.
     * @param path the path to find a codec for
     * @return a {@link ConfigCodec} instance which should be used to decode the file data, or null if none could be
     * found
     */
    protected abstract @Nullable ConfigCodec codecForPath(@NotNull Path path);
}
