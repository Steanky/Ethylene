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
     * @param wrapperFunction a function producing a wrapper exception from a generic {@link Throwable}
     * @param <TCall> the type object returned by the callable
     * @param <TErr> the type of exception to wrap
     * @return the value returned by the callable
     * @throws TErr if the callable throws an exception when its {@link Callable#call()} method is invoked
     * @throws NullPointerException if any of the arguments are null
     */
    public static <TCall, TErr extends Exception> TCall wrapException(@NotNull Callable<TCall> callable,
                                                                      @NotNull Class<TErr> exceptionClass,
                                                                      @NotNull Function<Throwable, TErr>
                                                                              wrapperFunction) throws TErr {
        Objects.requireNonNull(callable);
        Objects.requireNonNull(exceptionClass);
        Objects.requireNonNull(wrapperFunction);

        try {
            return callable.call();
        }
        catch (Exception exception) {
            if(exceptionClass.isInstance(exception)) {
                throw exceptionClass.cast(exception);
            }
            else {
                throw wrapperFunction.apply(exception);
            }
        }
    }
}
