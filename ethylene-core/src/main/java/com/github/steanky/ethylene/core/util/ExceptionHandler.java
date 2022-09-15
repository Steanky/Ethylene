package com.github.steanky.ethylene.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Utility class for handling and suppressing certain kinds of exceptions, usually to re-throw them at a later time.
 * This can be used in the context of multiple potentially exception-throwing operations, where the failure of one
 * operation should not prevent the others from running. To this end, it contains convenience methods which accept
 * variations of various interfaces in the {@link java.util.function} package which throw exceptions.
 *
 * @param <TErr> the type of exception to be handled
 */
public class ExceptionHandler<TErr extends Exception> {
    private final Class<TErr> exceptionClass;

    private TErr exception;

    /**
     * Creates a new instance of this class capable of handling exceptions assignable to the given exception class.
     *
     * @param exceptionClass the upper bound for exceptions
     */
    public ExceptionHandler(@NotNull Class<TErr> exceptionClass) {
        this.exceptionClass = Objects.requireNonNull(exceptionClass);
    }

    /**
     * Sets or suppresses the given exception. If no prior exception has been set, the given exception becomes the new
     * exception. If an exception was previously set, the new exception is suppressed by calling
     * {@link Throwable#addSuppressed(Throwable)} on the first exception.
     *
     * @param exception the exception to set or suppress
     */
    public void setOrSuppress(@NotNull TErr exception) {
        Objects.requireNonNull(exception);

        if (this.exception == null) {
            this.exception = exception;
        } else {
            this.exception.addSuppressed(exception);
        }
    }

    /**
     * Optionally gets the set exception. The {@link Optional} will be empty if no exception occurred, otherwise, it
     * will contain the set exception.
     *
     * @return an optional which will contain the exception if it has been set
     */
    public @NotNull Optional<TErr> getException() {
        return Optional.ofNullable(exception);
    }

    /**
     * Throws the set exception, if it exists.
     *
     * @throws TErr the exception type
     */
    public void throwIfPresent() throws TErr {
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Calls {@link ThrowingRunnable#run()} on the given function. If it throws an exception, it will be handled with
     * similar semantics to calling {@link ExceptionHandler#setOrSuppress(Exception)}.
     *
     * @param runnable the runnable to call
     */
    public void run(@NotNull ThrowingRunnable<? extends TErr> runnable) {
        Objects.requireNonNull(runnable);

        try {
            runnable.run();
        } catch (Exception e) {
            handleException(e);
        }
    }

    /**
     * Calls {@link ThrowingSupplier#get()} on the given function. If it throws an exception, it will be handled with
     * similar semantics to calling {@link ExceptionHandler#setOrSuppress(Exception)}, and the default {@link Supplier}
     * will be used to provide the return value of the function. If no exception is thrown, the return value of the
     * ThrowingSupplier is used.
     *
     * @param supplier the supplier to call
     * @param defaultSupplier the supplier used to provide the default value; only called if an exception occurs
     * @return the value returned by the default supplier if an exception occurred, otherwise the value returned by
     * the ThrowingSupplier
     *
     * @param <TReturn> the return value type
     */
    public <TReturn> TReturn get(@NotNull ThrowingSupplier<? extends TReturn, ? extends TErr> supplier,
            @NotNull Supplier<? extends TReturn> defaultSupplier) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(defaultSupplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            handleException(e);
        }

        return defaultSupplier.get();
    }

    /**
     * Calls {@link ThrowingFunction#apply(Object)} on the given function. If it throws an exception, it will be handled
     * with similar semantics to calling {@link ExceptionHandler#setOrSuppress(Exception)}, and the default
     * {@link Supplier} will be used to provide the return value of the function. If no exception is thrown, the return
     * value of the ThrowingFunction is used.
     *
     * @param function the function to call
     * @param defaultSupplier the supplier used to provide the default value; only called if an exception occurs
     * @return the value returned by the default supplier if an exception occurred, otherwise the value returned by
     * the ThrowingFunction
     *
     * @param <TReturn> the return value type
     */
    public <TAccept, TReturn> TReturn apply(
            @NotNull ThrowingFunction<? super TAccept, ? extends TReturn, ? extends TErr> function,
            TAccept value,
            @NotNull Supplier<? extends TReturn> defaultSupplier) {
        Objects.requireNonNull(function);
        Objects.requireNonNull(defaultSupplier);

        try {
            return function.apply(value);
        }
        catch (Exception e) {
            handleException(e);
        }

        return defaultSupplier.get();
    }

    private void handleException(Exception exception) {
        if (exceptionClass.isAssignableFrom(exception.getClass())) {
            setOrSuppress(exceptionClass.cast(exception));
        }
        else if (exception instanceof RuntimeException runtimeException) {
            //we don't handle runtime exceptions, but they can still occur
            throw runtimeException;
        }
        else {
            //should not happen under normal use
            throw new IllegalStateException("Unexpected exception type", exception);
        }
    }
}
