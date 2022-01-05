package com.github.steanky.ethylene.core;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Contains static utility methods for working with {@link Callable} objects.
 */
public final class CallableUtils {
    private CallableUtils() {
        throw new AssertionError("Why would you try to do this?");
    }

    /**
     * Calls the provided {@link Callable}. If it throws an exception which is an instance of the provided exception
     * class, the same exception will be re-thrown after casting to that type. If it throws an exception that is NOT
     * an instance of the provided exception class, the wrapper exception function will be called to generate a new
     * exception from the thrown one (typically by wrapping it, with the new exception having its cause set to the
     * old exception).
     * @param callable the callable to call
     * @param exceptionClass the type of exception to catch/wrap
     * @param wrapperException a function producing a wrapper exception from a generic {@link Throwable}
     * @param <R> the type object returned by the callable
     * @param <E> the type of exception to wrap
     * @return the object returned by the callable
     * @throws E if the callable throws an exception when its {@link Callable#call()} method is invoked
     * @throws NullPointerException if any of the arguments are null
     */
    public static <R, E extends Exception> @NotNull R wrapException(@NotNull Callable<R> callable,
                                                                    @NotNull Class<E> exceptionClass,
                                                                    @NotNull Function<Throwable, E> wrapperException)
            throws E {
        Objects.requireNonNull(callable);
        Objects.requireNonNull(exceptionClass);
        Objects.requireNonNull(wrapperException);

        try {
            return callable.call();
        }
        catch (Exception exception) {
            if(exceptionClass.isInstance(exception)) {
                throw exceptionClass.cast(exception);
            }
            else {
                throw wrapperException.apply(exception);
            }
        }
    }
}
