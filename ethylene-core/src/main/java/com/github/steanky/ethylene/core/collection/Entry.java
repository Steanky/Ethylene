package com.github.steanky.ethylene.core.collection;

import java.util.Map;

public interface Entry<TFirst, TSecond> {
    TFirst getFirst();

    TSecond getSecond();

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
