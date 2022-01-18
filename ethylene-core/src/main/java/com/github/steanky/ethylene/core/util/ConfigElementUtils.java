package com.github.steanky.ethylene.core.util;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.ConfigEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class used by {@link ConfigElement}. These methods are public for convenience, but not intended for general
 * use.
 */
public final class ConfigElementUtils {
    private ConfigElementUtils() { throw new AssertionError("Don't do this."); }

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
     * Static utility method used by {@link ConfigElement}. Acts as a wrapper for
     * {@link ConfigElement#getElement(Object...)}, and will use the value supplied by the {@link Supplier} if the path
     * does not exist, or the {@link Function} used to evaluate whether the type conversion may safely occur returns
     * false. Otherwise, the type conversion Function will be called and its result returned.
     * @param element the element to perform the operation on
     * @param returnSupplier the supplier used to supply return values if the path or type is invalid
     * @param typeValidator the function used to determine if the element type is valid
     * @param typeGetter the function used to actually convert the element
     * @param path the path to resolve
     * @param <TReturn> the generic return value type
     * @return the value stored in element at path, or the default value produced by the supplier
     * @throws NullPointerException if any of the arguments are null
     * @throws IllegalArgumentException if path contains invalid types (any besides Integer or String)
     */
    public static <TReturn> @NotNull TReturn getOrDefault(@NotNull ConfigElement element,
                                                          @NotNull Supplier<TReturn> returnSupplier,
                                                          @NotNull Function<ConfigElement, Boolean> typeValidator,
                                                          @NotNull Function<ConfigElement, TReturn> typeGetter,
                                                          @NotNull Object ... path) {
        Objects.requireNonNull(returnSupplier);
        Objects.requireNonNull(typeValidator);
        Objects.requireNonNull(typeGetter);

        ConfigElement newElement = element.getElement(path);

        if(newElement != null && typeValidator.apply(newElement)) {
            return typeGetter.apply(newElement);
        }
        else {
            return returnSupplier.get();
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
