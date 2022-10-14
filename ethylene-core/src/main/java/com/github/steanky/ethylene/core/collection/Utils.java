package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.Graph;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Function;

/**
 * Internal utilities for collections. Not part of the public API.
 */
final class Utils {
    /**
     * Deep-copies the provided {@link ConfigContainer}, maintaining the extract structure of the input tree, including
     * circular references, and the implementation types of every container encountered (when possible).
     * <p>
     * Immutable implementations of {@link ConfigContainer} will not be copied; the same instances will exist in the
     * input as well as the output tree.
     *
     * @param original the original
     * @return an exact copy of the input
     */
    static @NotNull ConfigContainer clone(@NotNull ConfigContainer original) {
        return (ConfigContainer) Graph.process(original, (ConfigElement node) -> {
            ConfigContainer configContainer = node.asContainer();
            Collection<ConfigEntry> entryCollection = configContainer.entryCollection();
            if (configContainer instanceof Immutable || configContainer instanceof ImmutableView) {
                //don't write anything to this accumulator
                return Graph.node(entryCollection.iterator(), Graph.output(configContainer, Graph
                    .emptyAccumulator()));
            }

            ConfigContainer result;
            try {
                //use the implementation's copy method...
                result = configContainer.emptyCopy();
            }
            catch (UnsupportedOperationException e) {
                //...unless we can't due to it not being supported, in which case use reasonable defaults
                int size = entryCollection.size();
                result = configContainer.isNode() ? new LinkedConfigNode(size) : new ArrayConfigList(size);
            }

            ConfigContainer out = result;
            return Graph.node(entryCollection.iterator(), Graph.output(out, (key, element, circular) -> {
                if (out.isNode()) {
                    out.asNode().put(key, element);
                }
                else {
                    out.asList().add(element);
                }
            }));
        }, ConfigElement::isContainer, Function.identity(), Graph.Options.TRACK_REFERENCES);
    }
}
