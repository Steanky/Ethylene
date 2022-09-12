package com.github.steanky.ethylene.core.util;

@FunctionalInterface
public interface ThrowingSupplier<TReturn, TErr extends Throwable> {
    TReturn get() throws TErr;
}
