package com.github.steanky.ethylene.collection;

import com.github.steanky.ethylene.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents an ordered collection of {@link ConfigElement} objects. ConfigList does not support null values.
 */
public interface ConfigList extends ConfigElement, List<ConfigElement> {
    @Override
    default boolean isList() {
        return true;
    }

    @Override
    default @NotNull ConfigList asList() {
        return this;
    }
}
