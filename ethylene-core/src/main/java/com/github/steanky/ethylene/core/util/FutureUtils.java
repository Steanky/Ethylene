package com.github.steanky.ethylene.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Contains utility methods for working with {@link CompletableFuture} objects.
 */
public final class FutureUtils {
    private FutureUtils() {throw new AssertionError("No.");}

    /**
     * Constructs a new {@link CompletableFuture} instance from the provided {@link Callable}. If the Callable throws an
     * exception, the CompletableFuture will be completed <i>exceptionally</i> through its
     * {@link CompletableFuture#completeExceptionally(Throwable)} method. Otherwise, it will complete normally with the
     * result of invoking {@link Callable#call()} on the provided Callable. The Callable will be executed asynchronously
     * using the provided {@link Executor}.
     *
     * @param callable the callable to invoke
     * @param executor the executor used to run the callable asynchronously
     * @param <TCall>  the object type returned by the callable
     * @return a {@link CompletableFuture} from the given Callable
     */
    public static <TCall> CompletableFuture<TCall> completeCallableAsync(@NotNull Callable<? extends TCall> callable,
            @NotNull Executor executor) {
        Objects.requireNonNull(callable);
        Objects.requireNonNull(executor);

        CompletableFuture<TCall> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                future.complete(callable.call());
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });

        return future;
    }

    /**
     * Works functionally the same as {@link FutureUtils#completeCallableAsync(Callable, Executor)}, but the given
     * {@link Callable} is run synchronously.
     *
     * @param callable the callable to invoke
     * @param <TCall>  the object returned by the callable
     * @return a {@link CompletableFuture} from the given Callable
     */
    public static <TCall> CompletableFuture<TCall> completeCallableSync(@NotNull Callable<? extends TCall> callable) {
        Objects.requireNonNull(callable);

        CompletableFuture<TCall> future = new CompletableFuture<>();
        try {
            future.complete(callable.call());
        } catch (Exception exception) {
            future.completeExceptionally(exception);
        }

        return future;
    }

    /**
     * Calls {@link Future#get()} on the provided {@link Future}. If an {@link ExecutionException} is thrown, the cause
     * of the exception is inspected. If it is an instance of the provided error class, the cause itself is thrown.
     * Otherwise, if cause is not an instance (or an {@link ExecutionException} is thrown instead), the provided mapping
     * function is called to wrap the exception.
     *
     * @param future           The future to call
     * @param exceptionWrapper The function used to map exceptions
     * @param errorClass       The exception class to throw
     * @param <TReturn>        The return value
     * @param <TErr>           The type of {@link Throwable} to throw
     * @return the result of Future#get()
     * @throws TErr if an ExecutionException or {@link InterruptedException} occurs when calling the future's get()
     *              method
     */
    public static <TReturn, TErr extends Throwable> TReturn getAndWrapException(
            @NotNull Future<? extends TReturn> future, @NotNull Function<Throwable, TErr> exceptionWrapper,
            @NotNull Class<TErr> errorClass) throws TErr {
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (errorClass.isInstance(cause)) {
                throw errorClass.cast(cause);
            } else {
                throw exceptionWrapper.apply(cause);
            }
        } catch (InterruptedException e) {
            throw exceptionWrapper.apply(e);
        }
    }
}
