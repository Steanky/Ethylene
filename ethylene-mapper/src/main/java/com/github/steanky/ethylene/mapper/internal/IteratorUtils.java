package com.github.steanky.ethylene.mapper.internal;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;

public class IteratorUtils {
    private IteratorUtils() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static class UnmodifiableIterator<TElement> implements Iterator<TElement> {
        private final Iterator<? extends TElement> underlying;

        private UnmodifiableIterator(Iterator<? extends TElement> underlying) {
            this.underlying = Objects.requireNonNull(underlying);
        }


        @Override
        public boolean hasNext() {
            return underlying.hasNext();
        }

        @Override
        public TElement next() {
            return underlying.next();
        }
    }

    public static <TElement> @NotNull Iterator<TElement> unmodifiable(@NotNull Iterator<TElement> target) {
        if (target instanceof UnmodifiableIterator) {
            return target;
        }

        return new UnmodifiableIterator<>(target);
    }
}
