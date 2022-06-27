package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents an ordered collection of {@link ConfigElement} objects. ConfigList does not support null values.
 */
public interface ConfigList extends ConfigElement, List<ConfigElement>, ConfigContainer {
    @Override
    default boolean isList() {
        return true;
    }

    @Override
    default @NotNull ConfigList asList() {
        return this;
    }

    /**
     * Convenience overload for {@link ConfigList#add(Object)}. The provided string value will be wrapped in a new
     * {@link ConfigPrimitive} and added to this list.
     * @param value the value to add to the node
     */
    default void addString(String value) {
        add(new ConfigPrimitive(value));
    }

    /**
     * Convenience overload for {@link ConfigList#add(Object)}. The provided number value will be wrapped in a new
     * {@link ConfigPrimitive} and added to this list.
     * @param value the value to add to the node
     */
    default void addNumber(Number value) {
        add(new ConfigPrimitive(value));
    }

    /**
     * Convenience overload for {@link ConfigList#add(Object)}. The provided char value will be wrapped in a new
     * {@link ConfigPrimitive} and added to this list.
     * @param value the value to add to the node
     */
    default void addCharacter(char value) {
        add(new ConfigPrimitive(value));
    }
}
