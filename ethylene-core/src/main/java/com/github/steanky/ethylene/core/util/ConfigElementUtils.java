package com.github.steanky.ethylene.core.util;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.Graph;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.ConfigEntry;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

/**
 * Simple utilities for ConfigElements.
 */
public final class ConfigElementUtils {
    private ConfigElementUtils() {
        throw new AssertionError("Why?");
    }

    private static String toStringInternal(ConfigElement input, IdentityHashMap<Object, Integer> identities) {
        if (input.isContainer()) {
            ConfigContainer configContainer = input.asContainer();
            Iterator<ConfigEntry> entryIterator = configContainer.entryCollection().iterator();

            if (!entryIterator.hasNext()) {
                return "$" + identities.size() + "{}";
            }

            StringBuilder builder = new StringBuilder().append('$').append(identities.size()).append('{');
            identities.put(configContainer, identities.size());

            while (true) {
                ConfigEntry entry = entryIterator.next();

                String key = entry.getKey();
                ConfigElement value = entry.getValue();

                if (key != null) {
                    builder.append(key).append('=');
                }

                if (value.isContainer() && identities.containsKey(value)) {
                    builder.append('$').append(identities.get(value));
                } else {
                    builder.append(toStringInternal(value, identities));
                }

                if (!entryIterator.hasNext()) {
                    return builder.append('}').toString();
                }

                builder.append(',').append(' ');
            }
        } else {
            return "[" + input + "]";
        }
    }

    /**
     * <p>Specialized helper method used by {@link ConfigContainer} implementations that need to override
     * {@link Object#toString()}. Supports circular and self-referential ConfigElement constructions by use of a "tag"
     * syntax: containers are associated with a <i>name</i>, and if a container occurs twice, it will be referred to by
     * the name the second time rather than showing its entire contents again.</p>
     *
     * @param input the input {@link ConfigElement} to show
     * @return the ConfigElement, represented as a string
     */
    public static String toString(@NotNull ConfigElement input) {
        Objects.requireNonNull(input);
        return toStringInternal(input, new IdentityHashMap<>());
    }

    /**
     * Deep-copies the provided {@link ConfigContainer}, maintaining the extract structure of the input tree, including
     * circular references.
     *
     * @param original the original
     * @return an exact copy of the input
     */
    public static @NotNull ConfigContainer clone(@NotNull ConfigContainer original) {
        return (ConfigContainer) Graph.process(original, (ConfigElement node) -> {
            ConfigContainer configContainer = node.asContainer();

            ConfigContainer result;
            try {
                //use the implementation's copy method...
                result = configContainer.emptyCopy();
            }
            catch (UnsupportedOperationException e) {
                //...unless we can't due to it not being supported, in which case use reasonable defaults
                int size = configContainer.entryCollection().size();
                result = configContainer.isNode() ? new LinkedConfigNode(size) : new ArrayConfigList(size);
            }

            ConfigContainer out = result;
            return Graph.node(configContainer.entryCollection().iterator(), Graph.output(out,
                (Graph.Accumulator<? super String, ? super ConfigElement>) (key, element, circular) -> {
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
