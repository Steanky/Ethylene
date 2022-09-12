package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.ElementType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Represents an ordered collection of {@link ConfigElement} objects. ConfigList does not support null values.
 */
public interface ConfigList extends ConfigElement, List<ConfigElement>, ConfigContainer {
    /**
     * Similarly to {@link ConfigNode#of(Object...)}, builds a new {@link ArrayConfigList} from the given object array.
     * Objects that are instances of {@link ConfigElement} will be added to the resulting list directly, whereas objects
     * that are not will be used in an attempt to create a new {@link ConfigPrimitive}.
     *
     * @param objects the object array to create a ConfigList from
     * @return a new, mutable ConfigList implementation
     */
    static @NotNull ConfigList of(Object @NotNull ... objects) {
        Objects.requireNonNull(objects);

        if (objects.length == 0) {
            return new ArrayConfigList(0);
        }

        ConfigList list = new ArrayConfigList(objects.length);
        for (Object object : objects) {
            if (object instanceof ConfigElement element) {
                list.add(element);
            } else {
                list.add(new ConfigPrimitive(object));
            }
        }

        return list;
    }

    @Override
    default boolean isList() {
        return true;
    }

    @Override
    default @NotNull ConfigList asList() {
        return this;
    }

    @Override
    default @NotNull ElementType type() {
        return ElementType.LIST;
    }

    /**
     * Convenience overload for {@link ConfigList#add(Object)}. The provided string value will be wrapped in a new
     * {@link ConfigPrimitive} and added to this list.
     *
     * @param value the value to add to the node
     */
    default void addString(String value) {
        add(new ConfigPrimitive(value));
    }

    /**
     * Convenience overload for {@link ConfigList#add(Object)}. The provided number value will be wrapped in a new
     * {@link ConfigPrimitive} and added to this list.
     *
     * @param value the value to add to the node
     */
    default void addNumber(Number value) {
        add(new ConfigPrimitive(value));
    }

    /**
     * Convenience overload for {@link ConfigList#add(Object)}. The provided char value will be wrapped in a new
     * {@link ConfigPrimitive} and added to this list.
     *
     * @param value the value to add to the node
     */
    default void addCharacter(char value) {
        add(new ConfigPrimitive(value));
    }

    /**
     * Convenience overload for {@link ConfigList#add(Object)}. The provided boolean value will be wrapped in a new
     * {@link ConfigPrimitive} and added to this list.
     *
     * @param value the value to add to the node
     */
    default void addBoolean(boolean value) {
        add(new ConfigPrimitive(value));
    }
}
