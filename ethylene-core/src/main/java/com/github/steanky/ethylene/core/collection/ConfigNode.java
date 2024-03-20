package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.ElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * <p>Represents some arbitrary configuration data in a tree-like structure. ConfigNode objects are mutable data
 * structures based off of {@link Map}, but contain additional features that aid in traversing hierarchies.</p>
 *
 * <p>ConfigNode objects do not permit null keys or values. The absence of a value can be represented with a
 * {@link ConfigPrimitive} instance containing null.</p>
 */
public interface ConfigNode extends ConfigElement, Map<String, ConfigElement>, ConfigContainer {
    /**
     * The shared, immutable empty ConfigNode.
     */
    ConfigNode EMPTY = ConfigContainers.EmptyImmutableConfigNode.INSTANCE;

    /**
     * Returns a new "defaulting" ConfigNode. If a value is not found in {@code base}, {@code defaults} will be queried
     * instead.
     * <p>
     * The returned node is immutable, but read-through to both {@code base} and {@code defaults}.
     *
     * @param base the base node
     * @param defaults the default node
     * @return a new ConfigNode
     */
    static @NotNull ConfigNode defaulting(@NotNull ConfigNode base, @NotNull ConfigNode defaults) {
        Objects.requireNonNull(base);
        Objects.requireNonNull(defaults);

        return new DefaultingConfigNode(base, defaults);
    }

    /**
     * Overload of {@link ConfigNode#of(Object...)}. Returns a new, empty {@link LinkedConfigNode}.
     *
     * @return a new, empty, mutable ConfigNode
     */
    static @NotNull ConfigNode of() {
        return new LinkedConfigNode(0);
    }

    /**
     * Overload of {@link ConfigNode#of(Object...)}. Returns a new {@link LinkedConfigNode} with an initial capacity of
     * 1, populated by the single given key and value.
     *
     * @param key   the key to initially populate the map with
     * @param value the value to initially populate the map with
     * @return a new, mutable ConfigNode containing exactly 1 entry
     */
    static @NotNull ConfigNode of(@NotNull String key, @Nullable Object value) {
        Objects.requireNonNull(key);

        ConfigNode node = new LinkedConfigNode(1);
        if (value instanceof ConfigElement element) {
            node.put(key, element);
        } else {
            node.put(key, ConfigPrimitive.of(value));
        }

        return node;
    }

    /**
     * Creates a new mutable, ordered ConfigNode implementation from the given object array. The array must be
     * even-length, with all even indices interpreted as keys, and all odd indices interpreted as the value
     * corresponding to the prior key. The value, if it is a ConfigElement, will be directly associated with the key,
     * otherwise, it will be used in an attempt to construct a {@link ConfigPrimitive} which will be associated with the
     * key. Therefore, the value must either be assignable to ConfigElement, or a valid type for the ConfigPrimitive
     * constructor.
     *
     * @param objects the object array to read
     * @return a new ordered ConfigNode implementation containing the objects present in the array, formatted as
     * specified above
     * @throws IllegalArgumentException if the array length is uneven, or if one of the even indices is not a string
     */
    static @NotNull ConfigNode of(Object @NotNull ... objects) {
        Objects.requireNonNull(objects);

        if (objects.length == 0) {
            return new LinkedConfigNode(0);
        }

        if (objects.length % 2 != 0) {
            throw new IllegalArgumentException("Must have an even number of arguments");
        }

        ConfigNode output = new LinkedConfigNode(objects.length / 2);
        for (int i = 0; i < objects.length; i += 2) {
            Object keyObject = objects[i];
            if (!(keyObject instanceof String keyString)) {
                throw new IllegalArgumentException(
                    "Key object must be string, was " + (keyObject == null ? "null" : keyObject.getClass().getName()));
            }

            Object valueObject = objects[i + 1];
            ConfigElement element;
            if (valueObject instanceof ConfigElement valueElement) {
                element = valueElement;
            } else {
                element = ConfigPrimitive.of(valueObject);
            }

            output.put(keyString, element);
        }

        return output;
    }

    @Override
    default @NotNull ElementType type() {
        return ElementType.NODE;
    }

    @Override
    default @NotNull ConfigNode asNode() {
        return this;
    }

    /**
     * Convenience overload for {@link ConfigNode#put(Object, Object)}. The provided string value will be wrapped in a
     * new {@link ConfigPrimitive} and added to this node.
     *
     * @param key   the key to be associated with this value
     * @param value the value to add to the node
     */
    default void putString(@NotNull String key, String value) {
        put(key, ConfigPrimitive.of(value));
    }

    /**
     * Convenience overload for {@link ConfigNode#put(Object, Object)}. The provided number value will be wrapped in a
     * new {@link ConfigPrimitive} and added to this node.
     *
     * @param key   the key to be associated with this value
     * @param value the value to add to the node
     */
    default void putNumber(@NotNull String key, Number value) {
        put(key, ConfigPrimitive.of(value));
    }

    /**
     * Convenience overload for {@link ConfigNode#put(Object, Object)}. The provided char value will be wrapped in a new
     * {@link ConfigPrimitive} and added to this node.
     *
     * @param key   the key to be associated with this value
     * @param value the value to add to the node
     */
    default void putCharacter(@NotNull String key, char value) {
        put(key, ConfigPrimitive.of(value));
    }

    /**
     * Convenience overload for {@link ConfigNode#put(Object, Object)}. The provided boolean value will be wrapped in a
     * new {@link ConfigPrimitive} and added to this node.
     *
     * @param key   the key to be associated with this value
     * @param value the value to add to the node
     */
    default void putBoolean(@NotNull String key, boolean value) {
        put(key, ConfigPrimitive.of(value));
    }

    @Override
    default @NotNull ConfigNode copy() {
        return (ConfigNode) ConfigContainer.super.copy();
    }

    @Override
    default @NotNull ConfigNode immutableCopy() {
        return (ConfigNode) ConfigContainer.super.immutableCopy();
    }

    @Override
    default @NotNull ConfigNode immutableView() {
        return (ConfigNode) ConfigContainer.super.immutableView();
    }
}
