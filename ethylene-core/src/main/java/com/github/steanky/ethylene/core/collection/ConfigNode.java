package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.ElementType;
import org.jetbrains.annotations.NotNull;

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
     * Creates a new ordered ConfigNode implementation from the given object array. The array must be even-length, with
     * all even indices interpreted as keys, and all odd indices interpreted as the value corresponding to the prior
     * key. The value, if it is a ConfigElement, will be directly associated with the key, otherwise, it will be used in
     * an attempt to construct a {@link ConfigPrimitive} which will be associated with the key. Therefore, the value
     * must either be assignable to ConfigElement, or a valid type for the ConfigPrimitive constructor.
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
                throw new IllegalArgumentException("Key object must be string, was " +
                        (keyObject == null ? "null" : keyObject.getClass().getName()));
            }

            Object valueObject = objects[i + 1];
            ConfigElement element;
            if (valueObject instanceof ConfigElement valueElement) {
                element = valueElement;
            } else {
                element = new ConfigPrimitive(valueObject);
            }

            output.put(keyString, element);
        }

        return output;
    }

    @Override
    default boolean isNode() {
        return true;
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
        put(key, new ConfigPrimitive(value));
    }

    /**
     * Convenience overload for {@link ConfigNode#put(Object, Object)}. The provided number value will be wrapped in a
     * new {@link ConfigPrimitive} and added to this node.
     *
     * @param key   the key to be associated with this value
     * @param value the value to add to the node
     */
    default void putNumber(@NotNull String key, Number value) {
        put(key, new ConfigPrimitive(value));
    }

    /**
     * Convenience overload for {@link ConfigNode#put(Object, Object)}. The provided char value will be wrapped in a new
     * {@link ConfigPrimitive} and added to this node.
     *
     * @param key   the key to be associated with this value
     * @param value the value to add to the node
     */
    default void putCharacter(@NotNull String key, char value) {
        put(key, new ConfigPrimitive(value));
    }

    /**
     * Convenience overload for {@link ConfigNode#put(Object, Object)}. The provided boolean value will be wrapped in a
     * new {@link ConfigPrimitive} and added to this node.
     *
     * @param key   the key to be associated with this value
     * @param value the value to add to the node
     */
    default void putBoolean(@NotNull String key, boolean value) {
        put(key, new ConfigPrimitive(value));
    }

    @Override
    default @NotNull ElementType type() {
        return ElementType.NODE;
    }
}
