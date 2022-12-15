package com.github.steanky.ethylene.mapper;

/**
 * A function that accepts four arguments and returns a value.
 *
 * @param <T> the type of the first argument
 * @param <U> the type of the second argument
 * @param <V> the type of the third argument
 * @param <W> the type of the fourth argument
 * @param <R> the return type
 */
public interface QuadFunction<T, U, V, W, R> {
    /**
     * Calls this function with the provided arguments.
     *
     * @param t the first argument
     * @param u the second argument
     * @param v the third argument
     * @param w the fourth argument
     * @return the return object
     */
    R apply(T t, U u, V v, W w);
}
