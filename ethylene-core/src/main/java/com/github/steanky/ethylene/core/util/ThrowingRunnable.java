package com.github.steanky.ethylene.core.util;

@FunctionalInterface
public interface ThrowingRunnable<TErr extends Exception> {
    void run() throws TErr;
}
