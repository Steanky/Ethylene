package com.github.steanky.ethylene.core.util;

@FunctionalInterface
public interface ThrowingRunnable<TErr extends Throwable> {
    void run() throws TErr;
}
