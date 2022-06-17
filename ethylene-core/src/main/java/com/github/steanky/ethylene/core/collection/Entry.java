package com.github.steanky.ethylene.core.collection;

import java.util.Map;

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
        };
    }
}
