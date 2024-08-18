package com.github.steanky.ethylene.core.collection;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a mutable key-value pair stored in a {@link ConfigContainer}. If the ConfigContainer is a
 * {@link ConfigList}, the key will be null. Otherwise, the key is guaranteed to be non-null.
 * <p>
 * While instances of this class are conceptually representative of an entry in a map or list, they will <i>never</i>
 * write-through to an underlying collection.
 */
public final class ConfigEntry implements Map.Entry<String, ConfigElement> {
    private String key;
    private ConfigElement element;

    private ConfigEntry(@Nullable String key, @NotNull ConfigElement element) {
        this.key = key;
        this.element = Objects.requireNonNull(element);
    }

    /**
     * Creates a new ConfigEntry instance.
     *
     * @param key     the key (name)
     * @param element the value element
     * @return a new ConfigEntry instance
     */
    public static @NotNull ConfigEntry of(@Nullable String key, @NotNull ConfigElement element) {
        return new ConfigEntry(key, element);
    }

    /**
     * Creates a new ConfigEntry instance with a null key.
     *
     * @param element the value element
     * @return a new ConfigEntry instance
     */
    public static @NotNull ConfigEntry of(@NotNull ConfigElement element) {
        return new ConfigEntry(null, element);
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

        if (obj instanceof Map.Entry<?, ?> entry) {
            return Objects.equals(key, entry.getKey()) && Objects.equals(element, entry.getValue());
        }

        return false;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public @NotNull ConfigElement getValue() {
        return element;
    }

    /**
     * Sets the key of this entry. This will not modify any underlying collection.
     *
     * @param key the new key
     * @return the old key
     */
    public String setKey(@Nullable String key) {
        String old = this.key;
        this.key = key;
        return old;
    }

    @Override
    public ConfigElement setValue(@NotNull ConfigElement value) {
        Objects.requireNonNull(value);

        ConfigElement old = element;
        this.element = value;
        return old;
    }
}
