package com.github.steanky.ethylene.core.collection;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * Convenience extension of {@link Map.Entry} that provides a static utility method for creating entries which support
 * null keys and values.
 * @param <TFirst> the key type
 * @param <TSecond> the value type
 */
public interface Entry<TFirst, TSecond> extends Map.Entry<TFirst, TSecond> {

    /**
     * Creates a new, immutable map entry which may have null keys and values.
     *
     * @param key the key object
     * @param value the value object
     * @return a new immutable entry
     * @param <TFirst> the key type
     * @param <TSecond> the value type
     */
    static <TFirst, TSecond> @NotNull Entry<TFirst, TSecond> of(TFirst key, TSecond value) {
        return new Entry<>() {
            @Override
            public TFirst getKey() {
                return key;
            }

            @Override
            public TSecond getValue() {
                return value;
            }

            @Override
            public int hashCode() {
                return Objects.hash(key, value);
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
                    return Objects.equals(key, entry.getKey()) && Objects.equals(value, entry.getValue());
                }

                return false;
            }

            @Override
            public String toString() {
                return "ImmutableEntry{key=" + key + ", value=" + value + "}";
            }
        };
    }

    @Override
    default TSecond setValue(TSecond value) {
        throw new UnsupportedOperationException("Immutable entry");
    }
}
