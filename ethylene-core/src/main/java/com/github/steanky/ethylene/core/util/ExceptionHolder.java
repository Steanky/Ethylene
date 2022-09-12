package com.github.steanky.ethylene.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

public class ExceptionHolder<TErr extends Throwable> {
    private TErr exception;

    public ExceptionHolder() {
        this.exception = null;
    }

    public void setOrSuppress(TErr throwable) {
        if (exception == null) {
            exception = throwable;
        }
        else {
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
        try {
            runnable.run();
        } catch (Throwable e) {
            setOrSuppress((TErr) e);
        }
    }

    @SuppressWarnings("unchecked")
    public <TReturn> TReturn supply(@NotNull ThrowingSupplier<? extends TReturn, ? extends TErr> supplier,
            @NotNull Supplier<? extends TReturn> defaultSupplier) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            setOrSuppress((TErr) e);
        }

        return defaultSupplier.get();
    }
}
