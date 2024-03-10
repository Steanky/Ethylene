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
     * The shared, immutable empty ConfigList.
     */
    ConfigList EMPTY = ConfigContainers.EmptyImmutableConfigList.INSTANCE;

    /**
     * Returns a new "defaulting" ConfigList. If a value at a certain index is not present in {@code base},
     * {@code defaults} will be used instead.
     * <p>
     * The returned node is immutable, but read-through to both {@code base} and {@code defaults}.
     *
     * @param base the base list
     * @param defaults the default list
     * @return a new ConfigList
     */
    static @NotNull ConfigList defaulting(@NotNull ConfigList base, @NotNull ConfigList defaults) {
        Objects.requireNonNull(base);
        Objects.requireNonNull(defaults);

        return new DefaultingConfigList(base, defaults);
    }

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
                list.add(ConfigPrimitive.of(object));
            }
        }

        return list;
    }

    @Override
    default @NotNull ElementType type() {
        return ElementType.LIST;
    }

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
     *
     * @param value the value to add to the node
     */
    default void addString(String value) {
        add(ConfigPrimitive.of(value));
    }

    /**
     * Convenience overload for {@link ConfigList#add(Object)}. The provided number value will be wrapped in a new
     * {@link ConfigPrimitive} and added to this list.
     *
     * @param value the value to add to the node
     */
    default void addNumber(Number value) {
        add(ConfigPrimitive.of(value));
    }

    /**
     * Convenience overload for {@link ConfigList#add(Object)}. The provided char value will be wrapped in a new
     * {@link ConfigPrimitive} and added to this list.
     *
     * @param value the value to add to the node
     */
    default void addCharacter(char value) {
        add(ConfigPrimitive.of(value));
    }

    /**
     * Convenience overload for {@link ConfigList#add(Object)}. The provided boolean value will be wrapped in a new
     * {@link ConfigPrimitive} and added to this list.
     *
     * @param value the value to add to the node
     */
    default void addBoolean(boolean value) {
        add(ConfigPrimitive.of(value));
    }

    @Override
    default @NotNull ConfigList copy() {
        return (ConfigList) ConfigContainer.super.copy();
    }

    @Override
    default @NotNull ConfigList immutableCopy() {
        return (ConfigList) ConfigContainer.super.immutableCopy();
    }

    @Override
    default @NotNull ConfigList immutableView() {
        return (ConfigList) ConfigContainer.super.immutableView();
    }
}
