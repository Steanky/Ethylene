package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.bridge.ConfigBridge;
import com.github.steanky.ethylene.core.util.FutureUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import com.github.steanky.ethylene.core.ConfigElement;

/**
 * Further specialization of {@link ConfigLoader} that uses a {@link ConfigProcessor} instance to marshal
 * {@link ConfigElement} instances to and from arbitrary data types. It stores a default type, which will be written
 * if implementations specify that their data source is currently <i>absent</i> (say, in case of a missing file).
 * @param <TData> the type of data to convert to and from
 */
public abstract class ProcessingConfigLoader<TData> implements ConfigLoader<TData> {
    private final ConfigProcessor<TData> processor;
    private final TData defaultData;

    /**
     * Constructs a new instance of ProcessingConfigLoader from the given {@link ConfigProcessor} and default data
     * object.
     * @param processor the processor to use
     * @param defaultData the default data object
     */
    public ProcessingConfigLoader(@NotNull ConfigProcessor<TData> processor, @NotNull TData defaultData) {
        this.processor = Objects.requireNonNull(processor);
        this.defaultData = Objects.requireNonNull(defaultData);
    }

    @Override
    public @NotNull CompletableFuture<Void> writeDefaultIfAbsent() {
        return FutureUtils.callableToCompletedFuture(() -> {
            if(isAbsent()) {
                getBridge().write(processor.elementFromData(defaultData));
            }

            return null;
        });
    }

    @Override
    public @NotNull CompletableFuture<TData> load() {
        return getBridge().read().thenCompose(element ->
                FutureUtils.callableToCompletedFuture(() -> processor.dataFromElement(element)));
    }

    /**
     * Gets the bridge used by this instance.
     * @return the bridge used by this instance
     */
    protected abstract @NotNull ConfigBridge getBridge();

    /**
     * Indicates whether this loader's data source is <i>absent</i>.
     * @return true if {@link ProcessingConfigLoader#writeDefaultIfAbsent()} should write a default object when called,
     * false otherwise
     */
    protected abstract boolean isAbsent();
}
