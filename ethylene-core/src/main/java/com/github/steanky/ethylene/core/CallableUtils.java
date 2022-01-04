package com.github.steanky.ethylene.core;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

public final class CallableUtils {
    private CallableUtils() {
        throw new AssertionError("Why would you try to do this?");
    }

    public static <R, E extends Throwable> @NotNull R wrapException(@NotNull Callable<R> callable,
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
