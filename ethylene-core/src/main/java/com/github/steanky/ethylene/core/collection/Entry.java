package com.github.steanky.ethylene.core.collection;

import java.util.Map;
import java.util.Objects;

public interface Entry<TFirst, TSecond> extends Map.Entry<TFirst, TSecond> {
    TFirst getFirst();

    TSecond getSecond();

    @Override
    default TFirst getKey() {
        return getFirst();
    }

    @Override
    default TSecond getValue() {
        return getSecond();
    }

    @Override
    default TSecond setValue(TSecond value) {
        throw new UnsupportedOperationException("Immutable entry");
    }

    static <TFirst, TSecond> Entry<TFirst, TSecond> of(TFirst first, TSecond second) {
        return new Entry<>() {
            @Override
            public TFirst getFirst() {
                return first;
            }

            @Override
            public TSecond getSecond() {
                return second;
            }

            @Override
            public int hashCode() {
                return Objects.hash(first, second);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }

                if (obj == this) {
                    return true;
                }

                if (obj instanceof Entry<?,?> entry) {
                    return Objects.equals(first, entry.getFirst()) && Objects.equals(second, entry.getSecond());
                }

                return false;
            }
        };
    }
}
