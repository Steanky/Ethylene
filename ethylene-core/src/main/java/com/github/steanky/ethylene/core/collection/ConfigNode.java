package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * <p>Represents some arbitrary configuration data in a tree-like structure. ConfigNode objects are mutable data
 * structures based off of {@link Map}, but contain additional features that aid in traversing hierarchies.</p>
 *
 * <p>ConfigNode objects do not permit null keys or values. The absence of a value can be represented with a
 * {@link ConfigPrimitive} instance containing null.</p>
 */
public interface ConfigNode extends ConfigElement, Map<String, ConfigElement>, ConfigContainer {
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
     * @param key the key to be associated with this value
     * @param value the value to add to the node
     */
    default void putString(@NotNull String key, String value) {
        put(key, new ConfigPrimitive(value));
    }

    /**
     * Convenience overload for {@link ConfigNode#put(Object, Object)}. The provided number value will be wrapped in a
     * new {@link ConfigPrimitive} and added to this node.
     * @param key the key to be associated with this value
     * @param value the value to add to the node
     */
    default void putNumber(@NotNull String key, Number value) {
        put(key, new ConfigPrimitive(value));
    }

    /**
     * Convenience overload for {@link ConfigNode#put(Object, Object)}. The provided char value will be wrapped in a
     * new {@link ConfigPrimitive} and added to this node.
     * @param key the key to be associated with this value
     * @param value the value to add to the node
     */
    default void putCharacter(@NotNull String key, char value) {
        put(key, new ConfigPrimitive(value));
    }

    /**
     * Convenience overload for {@link ConfigNode#put(Object, Object)}. The provided boolean value will be wrapped in a
     * new {@link ConfigPrimitive} and added to this node.
     * @param key the key to be associated with this value
     * @param value the value to add to the node
     */
    default void putBoolean(@NotNull String key, boolean value) {
        put(key, new ConfigPrimitive(value));
    }
}
