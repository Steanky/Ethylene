package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * <p>Represents some arbitrary configuration data in a tree-like structure. ConfigNode objects are mutable data
 * structures based off of {@link Map}, but contain additional features that aid in traversing hierarchies.</p>
 *
 * <p>ConfigNode objects do not permit null keys or values. The absence of a value can be represented with a
 * {@link ConfigPrimitive} instance containing null.</p>
 */
public interface ConfigNode extends ConfigElement, Map<String, ConfigElement> {
    /**
     * Obtains a {@link ConfigElement} from this node, possibly from a nested node (if multiple keys are provided). If
     * the "path" represented by the array of keys does not point to an element that exists, this method will return
     * null.
     * @throws NullPointerException if one or more keys are null
     * @throws IllegalArgumentException if the keys array is empty
     * @param keys The array of keys which represents a "path" leading to a specific ConfigElement
     * @return null if the path is not valid; otherwise, the ConfigElement pointed to by the path
     */
    ConfigElement getElement(@NotNull String... keys);

    @Override
    default boolean isNode() {
        return true;
    }

    @Override
    default @NotNull ConfigNode asNode() {
        return this;
    }
}
