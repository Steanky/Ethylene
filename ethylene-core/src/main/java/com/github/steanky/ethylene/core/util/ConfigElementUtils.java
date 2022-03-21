package com.github.steanky.ethylene.core.util;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.ConfigEntry;
import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Objects;

public final class ConfigElementUtils {
    private ConfigElementUtils() {
        throw new AssertionError("Why?");
    }

    private static String toStringInternal(ConfigElement input, IdentityHashMap<Object, Integer> identities) {
        if(input.isContainer()) {
            ConfigContainer configContainer = input.asContainer();
            Iterator<ConfigEntry> entryIterator = configContainer.entryCollection().iterator();

            if(!entryIterator.hasNext()) {
                return "$" + identities.size() + "{}";
            }

            StringBuilder builder = new StringBuilder().append('$').append(identities.size()).append('{');
            identities.put(configContainer, identities.size());

            while(true) {
                ConfigEntry entry = entryIterator.next();

                String key = entry.getKey();
                ConfigElement value = entry.getValue();

                if(key != null) {
                    builder.append(key).append('=');
                }

                if(value.isContainer() && identities.containsKey(value)) {
                    builder.append('$').append(identities.get(value));
                }
                else {
                    builder.append(toStringInternal(value, identities));
                }

                if(!entryIterator.hasNext()) {
                    return builder.append('}').toString();
                }

                builder.append(',').append(' ');
            }
        }
        else {
            return "[" + input + "]";
        }
    }

    /**
     * <p>Specialized helper method used by {@link ConfigContainer} implementations that need to override
     * {@link Object#toString()}. Supports circular and self-referential ConfigElement constructions by use of a "tag"
     * syntax: containers are associated with a <i>name</i>, and if a container occurs twice, it will be referred to by
     * the name the second time rather than showing its entire contents again.</p>
     * @param input the input {@link ConfigElement} to show
     * @return the ConfigElement, represented as a string
     */
    public static String toString(@NotNull ConfigElement input) {
        Objects.requireNonNull(input);
        return toStringInternal(input, new IdentityHashMap<>());
    }
}
