package com.github.steanky.ethylene.core.util;

import java.util.function.Supplier;

/**
 * A {@link Supplier}-like function that may throw a specific kind of exception.
 *
 * @param <TReturn> the return type
 * @param <TErr> the exception type
 */
@FunctionalInterface
public interface ThrowingSupplier<TReturn, TErr extends Exception> {
    /**
     * Gets the value from this supplier.
     *
     * @return the value
     * @throws TErr if the value cannot be retrieved for any reason
     */
    TReturn get() throws TErr;
}
