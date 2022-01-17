package com.github.steanky.ethylene.core.databind;

import com.github.steanky.ethylene.core.collection.ConfigNode;
import org.jetbrains.annotations.NotNull;

/**
 * Converts a {@link ConfigNode} object into a representative Java object, and vice-versa.
 */
public interface ObjectMapper {
    <TReturn> @NotNull TReturn mapNode(@NotNull ConfigNode node, @NotNull Class<TReturn> returnClass);

    @NotNull ConfigNode mapObject(@NotNull Object object);
}

