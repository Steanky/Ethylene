package com.github.steanky.ethylene.core.util;

import java.util.function.Function;

/**
 * A {@link Function}-like interface that may throw an exception when applying.
 *
 * @param <T> the input type
 * @param <R> the return type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {
    /**
     * Calls this function with the provided input.
     *
     * @param t the input object
     * @return the output object
     * @throws E if a return value cannot be computed
     */
    R apply(T t) throws E;
}
