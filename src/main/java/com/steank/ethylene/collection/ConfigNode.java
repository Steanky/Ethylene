package com.steank.ethylene.collection;

import com.steank.ethylene.ConfigElement;
import com.steank.ethylene.ConfigPrimitive;
import com.steank.ethylene.ElementType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

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
     * the "path" represented by the array of keys is not valid (one or more elements does not exist), an empty
     * {@link Optional} will be returned. Otherwise, the Optional will contain the element referred to by the path.
     * @throws NullPointerException if one or more keys are null
     * @throws IllegalArgumentException if the keys array is empty
     * @param keys The array of keys which represents a "path" leading to a specific ConfigElement
     * @return An Optional, which will be empty if one or more of the provided elements does not exist; otherwise,
     * it will contain the ConfigElement pointed to by the path
     */
    @NotNull Optional<ConfigElement> getElement(@NotNull String... keys);

    @Override
    default @NotNull ConfigNode asConfigNode() {
        return this;
    }

    @Override
    default @NotNull ElementType getType() {
        return ElementType.NODE;
    }
}
