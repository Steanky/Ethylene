package com.github.steanky.ethylene.core.util;

/**
 * A {@link Runnable}-like function which may throw an exception.
 *
 * @param <TErr> the error type
 */
@FunctionalInterface
public interface ThrowingRunnable<TErr extends Exception> {
    /**
     * Calls this ThrowingRunnable.
     *
     * @throws TErr if an error occurs while running
     */
    void run() throws TErr;
}
