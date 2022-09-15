package com.github.steanky.ethylene.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class ExceptionHolder<TErr extends Throwable> {
    private final Class<TErr> exceptionClass;

    private TErr exception;

    public ExceptionHolder(@NotNull Class<TErr> exceptionClass) {
        this.exceptionClass = Objects.requireNonNull(exceptionClass);
    }

    public void setOrSuppress(@NotNull TErr throwable) {
        Objects.requireNonNull(throwable);

        if (exception == null) {
            exception = throwable;
        } else {
            exception.addSuppressed(throwable);
        }
    }

    public @NotNull Optional<TErr> getException() {
        return Optional.ofNullable(exception);
    }

    public void throwIfPresent() throws TErr {
        if (exception != null) {
            throw exception;
        }
    }

    public void call(@NotNull ThrowingRunnable<? extends TErr> runnable) {
        Objects.requireNonNull(runnable);

        try {
            runnable.run();
        } catch (Throwable e) {
            handle(e);
        }
    }

    public <TReturn> TReturn supply(@NotNull ThrowingSupplier<? extends TReturn, ? extends TErr> supplier,
            @NotNull Supplier<? extends TReturn> defaultSupplier) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(defaultSupplier);

        try {
            return supplier.get();
        } catch (Throwable e) {
            handle(e);
        }

        return defaultSupplier.get();
    }

    private void handle(Throwable throwable) {
        if (exceptionClass.isAssignableFrom(throwable.getClass())) {
            setOrSuppress(exceptionClass.cast(throwable));
        }
        else if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
    }
}
