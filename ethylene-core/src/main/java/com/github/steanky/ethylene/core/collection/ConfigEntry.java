package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * <p>Represents a particular key-value pair stored in a {@link ConfigContainer}. If the ConfigContainer is like a
 * list, the key will be null.</p>
 */
public final class ConfigEntry implements Entry<String, ConfigElement> {
    private final String key;
    private final ConfigElement element;

    /**
     * Creates a new ConfigEntry instance.
     *
     * @param key     the key (name)
     * @param element the value element
     */
    public ConfigEntry(@Nullable String key, @NotNull ConfigElement element) {
        this.key = key;
        this.element = Objects.requireNonNull(element);
    }

    @Override
    public String getFirst() {
        return key;
    }

    @Override
    public @NotNull ConfigElement getSecond() {
        return element;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, element);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof Entry<?, ?> entry) {
            return Objects.equals(key, entry.getFirst()) && Objects.equals(element, entry.getSecond());
        }

        return false;
    }
}
