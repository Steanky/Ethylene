package com.github.steanky.ethylene.core.processor;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

/**
 * Processes some configuration data. Fundamentally, implementations of this interface act as simple bidirectional
 * mapping functions between {@link ConfigElement} instances and arbitrary data.
 * @param <TData> the type of data to convert to and from
 */
public interface ConfigProcessor<TData> {
    /**
     * Produces some data from a provided {@link ConfigElement}.
     * @param element the element to process
     * @return the data object
     * @throws ConfigProcessException if the provided {@link ConfigElement} does not contain valid data
     */
    TData dataFromElement(@NotNull ConfigElement element) throws ConfigProcessException;

    /**
     * Produces a {@link ConfigElement} from the provided data object.
     * @param data the data object
     * @return a {@link ConfigElement} representing the given data
     * @throws ConfigProcessException if the data is invalid
     */
    @NotNull ConfigElement elementFromData(TData data) throws ConfigProcessException;
}
