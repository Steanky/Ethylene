package com.steank.ethylene.collection;

import com.steank.ethylene.ConfigElement;
import com.steank.ethylene.ElementType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents an ordered collection of {@link ConfigElement} objects. ConfigList does not support null values.
 */
public interface ConfigList extends ConfigElement, List<ConfigElement> {
    @Override
    default @NotNull ConfigList asConfigList() {
        return this;
    }

    default @NotNull ElementType getType() {
        return ElementType.ARRAY;
    }
}
