package com.github.steanky.ethylene.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class ExceptionHolder<TErr extends Throwable> {
    private TErr exception;

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

    @SuppressWarnings("unchecked")
    public void call(@NotNull ThrowingRunnable<? extends TErr> runnable) {
        Objects.requireNonNull(runnable);

        try {
            runnable.run();
        } catch (Throwable e) {
            setOrSuppress((TErr) e);
        }
    }

    @SuppressWarnings("unchecked")
    public <TReturn> TReturn supply(@NotNull ThrowingSupplier<? extends TReturn, ? extends TErr> supplier,
            @NotNull Supplier<? extends TReturn> defaultSupplier) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(defaultSupplier);

        try {
            return supplier.get();
        } catch (Throwable e) {
            //can't catch a type parameter, this cast is necessary and safe
            setOrSuppress((TErr) e);
        }

        return defaultSupplier.get();
    }
}
