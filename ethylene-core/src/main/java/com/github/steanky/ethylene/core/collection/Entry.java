package com.github.steanky.ethylene.core.collection;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * Convenience extension of {@link Map.Entry} that provides static utility methods for creating entries which support
 * null keys and values.
 * <p>
 * In addition to complying with the general contract of {@link Map.Entry}, implementations must be equality-comparable
 * to all other {@link Map.Entry}.
 *
 * @param <TKey>   the key type
 * @param <TValue> the value type
 */
public interface Entry<TKey, TValue> extends Map.Entry<TKey, TValue> {
    /**
     * Creates a new, immutable key and value map entry which may have null keys and values.
     *
     * @param key       the key object
     * @param value     the value object
     * @param <TFirst>  the key type
     * @param <TSecond> the value type
     * @return a new immutable entry
     */
    static <TFirst, TSecond> Map.@NotNull Entry<TFirst, TSecond> of(TFirst key, TSecond value) {
        return new AbstractEntry<>() {
            @Override
            public TFirst getKey() {
                return key;
            }

            @Override
            public TSecond getValue() {
                return value;
            }

            @Override
            public String toString() {
                return "ImmutableEntry{key=" + key + ", value=" + value + "}";
            }
        };
    }

    /**
     * Creates a new, mutable key and value map entry which may have null keys and values.
     *
     * @param key       the initial key object
     * @param value     the initial value object
     * @param <TFirst>  the key type
     * @param <TSecond> the value type
     * @return a new mutable entry
     */
    static <TFirst, TSecond> Map.@NotNull Entry<TFirst, TSecond> mutable(TFirst key, TSecond value) {
        return new AbstractEntry<>() {
            private TFirst key;
            private TSecond value;

            @Override
            public TFirst getKey() {
                return key;
            }

            @Override
            public TSecond getValue() {
                return value;
            }

            @Override
            public TSecond setValue(TSecond value) {
                TSecond old = this.value;
                this.value = value;
                return old;
            }

            @Override
            public TFirst setKey(TFirst key) {
                TFirst old = this.key;
                this.key = key;
                return old;
            }

            @Override
            public String toString() {
                return "MutableEntry{key=" + key + ", value=" + value + "}";
            }
        };
    }

    @Override
    default TValue setValue(TValue value) {
        throw new UnsupportedOperationException("Value-immutable entry");
    }

    /**
     * Sets the key value.
     *
     * @param key the new key value
     * @return the old key
     * @throws UnsupportedOperationException if this is not an immutable {@link Entry}
     */
    default TKey setKey(TKey key) {
        throw new UnsupportedOperationException("Key-immutable entry");
    }

    /**
     * Abstract implementation of {@link Entry} which supplies basic {@link Object#hashCode()} and
     * {@link Object#equals(Object)} methods that comply with the general contract of {@link Map.Entry}.
     *
     * @param <TKey>   the key type
     * @param <TValue> the value type
     */
    abstract class AbstractEntry<TKey, TValue> implements Entry<TKey, TValue> {
        @Override
        public final int hashCode() {
            TKey key = getKey();
            TValue value = getValue();

            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        @Override
        public final boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (obj == this) {
                return true;
            }

            if (obj instanceof Map.Entry<?, ?> entry) {
                return Objects.equals(getKey(), entry.getKey()) && Objects.equals(getValue(), entry.getValue());
            }

            return false;
        }
    }
}
