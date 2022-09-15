package com.github.steanky.ethylene.core.util;

@FunctionalInterface
public interface ThrowingSupplier<TReturn, TErr extends Exception> {
    TReturn get() throws TErr;
}
