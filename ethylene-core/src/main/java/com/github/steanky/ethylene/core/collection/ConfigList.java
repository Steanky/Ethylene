package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.ElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;
import java.util.function.Supplier;

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
     * Overload of {@link ConfigList#of(Object...)}. Returns a new, empty {@link ArrayConfigList}.
     *
     * @return a new, empty, mutable ConfigList
     */
    static @NotNull ConfigList of() {
        return new ArrayConfigList(0);
    }

    /**
     * Overload of {@link ConfigList#of(Object...)}. Returns a new {@link ArrayConfigList} with an initial capacity of
     * 1, populated by the single value.
     *
     * @param value the value to initially populate the list with
     * @return a new, mutable ConfigList containing exactly 1 object
     */
    static @NotNull ConfigList of(@Nullable Object value) {
        ConfigList list = new ArrayConfigList(1);
        if (value instanceof ConfigElement element) {
            list.add(element);
        }
        else {
            list.add(ConfigPrimitive.of(value));
        }

        return list;
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

    /**
     * Overload of {@link ConfigList#immutable(Object...)}. Returns the shared empty immutable list.
     *
     * @return the empty immutable list {@link ConfigList#EMPTY}
     */
    static @NotNull ConfigList immutable() {
        return EMPTY;
    }

    /**
     * Overload of {@link ConfigList#immutable(Object...)}. Returns an immutable list with only a single entry.
     *
     * @param value the value associated with the key
     * @return a new immutable list with one element
     */
    static @NotNull ConfigList immutable(@Nullable Object value) {
        ConfigElement[] trusted = new ConfigElement[1];

        if (value instanceof ConfigContainer container) {
            trusted[0] = container.immutableCopy();
        }
        else if (value instanceof ConfigElement configElement) {
            trusted[0] = configElement;
        }
        else {
            trusted[0] = ConfigPrimitive.of(value);
        }

        return new ConfigContainers.ImmutableConfigList(trusted);
    }

    /**
     * Similarly to {@link ConfigNode#immutable(Object...)}, builds a new {@link ArrayConfigList} from the given object
     * array. Objects that are instances of {@link ConfigElement} will be added to the resulting list directly, whereas
     * objects that are not will be used in an attempt to create a new {@link ConfigPrimitive}. All
     * {@link ConfigContainer}s present in the array will be copied immutably.
     *
     * @param objects the object array to create a ConfigList from
     * @return a new, immutable ConfigList implementation
     */
    static @NotNull ConfigList immutable(Object @NotNull ... objects) {
        Objects.requireNonNull(objects);

        if (objects.length == 0) {
            return EMPTY;
        }

        ConfigElement[] trusted = new ConfigElement[objects.length];
        for (int i = 0; i < objects.length; i++) {
            Object value = objects[i];
            if (value instanceof ConfigContainer container) {
                trusted[i] = container.immutableCopy();
            }
            else if (value instanceof ConfigElement element) {
                trusted[i] = element;
            }
            else {
                trusted[i] = ConfigPrimitive.of(value);
            }
        }

        return new ConfigContainers.ImmutableConfigList(trusted);
    }

    @Override
    default @NotNull ElementType type() {
        return ElementType.LIST;
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
    default void addString(@Nullable String value) {
        add(ConfigPrimitive.of(value));
    }

    /**
     * Convenience overload for {@link ConfigList#add(Object)}. The provided number value will be wrapped in a new
     * {@link ConfigPrimitive} and added to this list.
     *
     * @param value the value to add to the node
     */
    default void addNumber(@Nullable Number value) {
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

    /**
     * Gets a value if it is in-bounds and the expected type; else returns the non-null default value.
     *
     * @param index the index
     * @param defaultValue the default value
     * @return the value present in this node if it is the right type and in-bounds; else the default value
     */
    default @NotNull ConfigContainer getContainerOrDefault(int index, @NotNull ConfigContainer defaultValue) {
        Objects.requireNonNull(defaultValue);
        if (index < 0 || index >= size()) {
            return defaultValue;
        }

        ConfigElement element = get(index);
        if (!element.isContainer()) {
            return defaultValue;
        }

        return element.asContainer();
    }

    /**
     * Gets a value if it is in-bounds and the expected type; else computes and returns the non-null default value.
     *
     * @param index the index
     * @param defaultValueSupplier the default value supplier
     * @return the value present in this node if it is the right type and in-bounds; else the non-null computed default
     * value
     */
    default @NotNull ConfigContainer getContainerOrDefault(int index,
        @NotNull Supplier<? extends @NotNull ConfigContainer> defaultValueSupplier) {
        Objects.requireNonNull(defaultValueSupplier);
        if (index < 0 || index >= size()) {
            return Objects.requireNonNull(defaultValueSupplier.get(), "default value supplier");
        }

        ConfigElement element = get(index);
        if (!element.isContainer()) {
            return Objects.requireNonNull(defaultValueSupplier.get(), "default value supplier");
        }

        return element.asContainer();
    }

    /**
     * Gets a value if it is in-bounds and the expected type; else returns the non-null default value.
     *
     * @param index the index
     * @param defaultValue the default value
     * @return the value present in this node if it is the right type and in-bounds; else the default value
     */
    default @NotNull ConfigNode getNodeOrDefault(int index, @NotNull ConfigNode defaultValue) {
        Objects.requireNonNull(defaultValue);
        if (index < 0 || index >= size()) {
            return defaultValue;
        }

        ConfigElement element = get(index);
        if (!element.isNode()) {
            return defaultValue;
        }

        return element.asNode();
    }

    /**
     * Gets a value if it is in-bounds and the expected type; else computes and returns the non-null default value.
     *
     * @param index the index
     * @param defaultValueSupplier the default value supplier
     * @return the value present in this node if it is the right type and in-bounds; else the non-null computed default
     * value
     */
    default @NotNull ConfigNode getNodeOrDefault(int index,
        @NotNull Supplier<? extends @NotNull ConfigNode> defaultValueSupplier) {
        Objects.requireNonNull(defaultValueSupplier);
        if (index < 0 || index >= size()) {
            return Objects.requireNonNull(defaultValueSupplier.get(), "default value supplier");
        }

        ConfigElement element = get(index);
        if (!element.isNode()) {
            return Objects.requireNonNull(defaultValueSupplier.get(), "default value supplier");
        }

        return element.asNode();
    }

    /**
     * Gets a value if it is in-bounds and the expected type; else returns the non-null default value.
     *
     * @param index the index
     * @param defaultValue the default value
     * @return the value present in this node if it is the right type and in-bounds; else the default value
     */
    default @NotNull ConfigList getListOrDefault(int index, @NotNull ConfigList defaultValue) {
        Objects.requireNonNull(defaultValue);
        if (index < 0 || index >= size()) {
            return defaultValue;
        }

        ConfigElement element = get(index);
        if (!element.isList()) {
            return defaultValue;
        }

        return element.asList();
    }

    /**
     * Gets a value if it is in-bounds and the expected type; else computes and returns the non-null default value.
     *
     * @param index the index
     * @param defaultValueSupplier the default value supplier
     * @return the value present in this node if it is the right type and in-bounds; else the non-null computed default
     * value
     */
    default @NotNull ConfigList getListOrDefault(int index,
        @NotNull Supplier<? extends @NotNull ConfigList> defaultValueSupplier) {
        Objects.requireNonNull(defaultValueSupplier);
        if (index < 0 || index >= size()) {
            return Objects.requireNonNull(defaultValueSupplier.get(), "default value supplier");
        }

        ConfigElement element = get(index);
        if (!element.isList()) {
            return Objects.requireNonNull(defaultValueSupplier.get(), "default value supplier");
        }

        return element.asList();
    }

    /**
     * Gets a value if it is in-bounds and the expected type; else returns the default value.
     *
     * @param index the index
     * @param defaultValue the default value
     * @return the value present in this node if it is the right type and in-bounds; else the default value
     */
    default boolean getBooleanOrDefault(int index, boolean defaultValue) {
        if (index < 0 || index >= size()) {
            return defaultValue;
        }

        ConfigElement element = get(index);
        if (!element.isBoolean()) {
            return defaultValue;
        }

        return element.asBoolean();
    }

    /**
     * Gets a value if it is in-bounds and the expected type; else computes and returns the default value.
     *
     * @param index the index
     * @param defaultValueSupplier the default value supplier
     * @return the value present in this node if it is the right type and in-bounds; else the non-null computed default
     * value
     */
    default boolean getBooleanOrDefault(int index,
        @NotNull BooleanSupplier defaultValueSupplier) {
        Objects.requireNonNull(defaultValueSupplier);
        if (index < 0 || index >= size()) {
            return defaultValueSupplier.getAsBoolean();
        }

        ConfigElement element = get(index);
        if (!element.isBoolean()) {
            return defaultValueSupplier.getAsBoolean();
        }

        return element.asBoolean();
    }

    /**
     * Gets a value if it is in-bounds and the expected type; else returns the non-null default value.
     *
     * @param index the index
     * @param defaultValue the default value
     * @return the value present in this node if it is the right type and in-bounds; else the default value
     */
    default @NotNull Number getNumberOrDefault(int index, @NotNull Number defaultValue) {
        Objects.requireNonNull(defaultValue);
        if (index < 0 || index >= size()) {
            return defaultValue;
        }

        ConfigElement element = get(index);
        if (!element.isNumber()) {
            return defaultValue;
        }

        return element.asNumber();
    }

    /**
     * Gets a value if it is in-bounds and the expected type; else computes and returns the non-null default value.
     *
     * @param index the index
     * @param defaultValueSupplier the default value supplier
     * @return the value present in this node if it is the right type and in-bounds; else the non-null computed default
     * value
     */
    default @NotNull Number getNumberOrDefault(int index,
        @NotNull Supplier<? extends @NotNull Number> defaultValueSupplier) {
        Objects.requireNonNull(defaultValueSupplier);
        if (index < 0 || index >= size()) {
            return Objects.requireNonNull(defaultValueSupplier.get(), "default value supplier");
        }

        ConfigElement element = get(index);
        if (!element.isNumber()) {
            return Objects.requireNonNull(defaultValueSupplier.get(), "default value supplier");
        }

        return element.asNumber();
    }

    /**
     * Gets a value if it is in-bounds and the expected type; else returns the non-null default value.
     *
     * @param index the index
     * @param defaultValue the default value
     * @return the value present in this node if it is the right type and in-bounds; else the default value
     */
    default @NotNull String getStringOrDefault(int index, @NotNull String defaultValue) {
        Objects.requireNonNull(defaultValue);
        if (index < 0 || index >= size()) {
            return defaultValue;
        }

        ConfigElement element = get(index);
        if (!element.isString()) {
            return defaultValue;
        }

        return element.asString();
    }

    /**
     * Gets a value if it is in-bounds and the expected type; else computes and returns the non-null default value.
     *
     * @param index the index
     * @param defaultValueSupplier the default value supplier
     * @return the value present in this node if it is the right type and in-bounds; else the non-null computed default
     * value
     */
    default @NotNull String getStringOrDefault(int index,
        @NotNull Supplier<? extends @NotNull String> defaultValueSupplier) {
        Objects.requireNonNull(defaultValueSupplier);
        if (index < 0 || index >= size()) {
            return Objects.requireNonNull(defaultValueSupplier.get(), "default value supplier");
        }

        ConfigElement element = get(index);
        if (!element.isString()) {
            return Objects.requireNonNull(defaultValueSupplier.get(), "default value supplier");
        }

        return element.asString();
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
    @NotNull
    default ConfigList mutableCopy() {
        return (ConfigList) ConfigContainer.super.mutableCopy();
    }

    @Override
    @NotNull
    default ConfigList mutableCopy(@NotNull IntFunction<? extends @NotNull ConfigNode> configNodeCreator,
        @NotNull IntFunction<? extends @NotNull ConfigList> configListCreator) {
        return (ConfigList) ConfigContainer.super.mutableCopy(configNodeCreator, configListCreator);
    }

    @Override
    default @NotNull ConfigList immutableView() {
        return (ConfigList) ConfigContainer.super.immutableView();
    }
}
