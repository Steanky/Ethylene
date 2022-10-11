package com.github.steanky.ethylene.core.loader;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.bridge.ConfigSource;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.github.steanky.ethylene.core.util.FutureUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * {@link ConfigLoader} implementation that uses a {@link ConfigProcessor} instance to marshal {@link ConfigElement}s to
 * and from arbitrary data types. It stores a default type, which will be written if implementations specify that their
 * data in is currently <i>absent</i> (say, in case of a missing file) and
 * {@link ProcessingConfigLoader#writeDefaultIfAbsent()} is invoked.
 *
 * @param <TData> the type of data to convert to and from
 */
public abstract class ProcessingConfigLoader<TData> implements ConfigLoader<TData> {
    private final ConfigProcessor<TData> processor;
    private final TData defaultData;
    private final ConfigSource source;

    /**
     * Constructs a new instance of ProcessingConfigLoader from the given {@link ConfigProcessor} and default data
     * object.
     *
     * @param processor   the processor to use
     * @param defaultData the default data object
     * @param source      the {@link ConfigSource} used for reading/writing data
     */
    public ProcessingConfigLoader(@NotNull ConfigProcessor<TData> processor, @NotNull TData defaultData,
        @NotNull ConfigSource source) {
        this.processor = Objects.requireNonNull(processor);
        this.defaultData = Objects.requireNonNull(defaultData);
        this.source = Objects.requireNonNull(source);
    }

    @Override
    public @NotNull CompletableFuture<Void> writeDefaultIfAbsent() {
        if (isAbsent()) {
            //elementFromData will run synchronously in all cases, bridge::write MAY run asynchronously
            return FutureUtils.completeCallableSync(() -> processor.elementFromData(defaultData))
                .thenCompose(source::write);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull CompletableFuture<TData> load() {
        //bridge.read() MAY run asynchronously, dataFromElement will always run synchronously
        return source.read()
            .thenCompose(element -> FutureUtils.completeCallableSync(() -> processor.dataFromElement(element)));
    }

    @Override
    public @NotNull CompletableFuture<Void> write(@NotNull TData data) {
        return FutureUtils.completeCallableSync(() -> processor.elementFromData(defaultData))
            .thenCompose(source::write);
    }

    /**
     * Indicates whether this loader's data in is <i>absent</i>.
     *
     * @return true if {@link ProcessingConfigLoader#writeDefaultIfAbsent()} should write a default object when called,
     * false otherwise
     */
    protected abstract boolean isAbsent();
}
