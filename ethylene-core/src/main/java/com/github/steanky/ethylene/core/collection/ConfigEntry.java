package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * <p>Represents a particular key-value pair stored in a {@link ConfigContainer}. If the ConfigContainer is like a list,
 * the key will be null.</p>
 *
 * <p>This class is not a record because it should have a package-private constructor (there should never be a need for
 * an API user to create an instance of this class).</p>
 */
@SuppressWarnings("ClassCanBeRecord")
public final class ConfigEntry {
    private final String key;
    private final ConfigElement element;

    /**
     * Creates a new ConfigEntry instance.
     * @param key the key (name)
     * @param element the value element
     */
    ConfigEntry(String key, @NotNull ConfigElement element) {
        this.key = key;
        this.element = Objects.requireNonNull(element);
    }

    /**
     * Returns the key (name) of this ConfigEntry.
     * @return the key for this ConfigMember
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the {@link ConfigElement} (value) of this ConfigEntry.
     * @return the value for this ConfigEntry
     */
    public @NotNull ConfigElement getValue() {
        return element;
    }
}
