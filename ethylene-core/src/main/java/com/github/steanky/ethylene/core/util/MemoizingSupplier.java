package com.github.steanky.ethylene.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Supplier which lazily produces and caches a given result from an underlying supplier.
 *
 * @param <TData> the type of data returned by the supplier
 */
public final class MemoizingSupplier<TData> implements Supplier<TData> {
    private final Supplier<? extends TData> supplier;

    //distinguish between null data and absent data
    private boolean hasData;
    private TData cached;

    private MemoizingSupplier(@NotNull Supplier<? extends TData> supplier) {
        this.supplier = supplier;
    }

    /**
     * Creates a new {@link Supplier} from the given supplier. The new supplier will cache the result of the
     * underlying supplier such that it will only be called once.
     *
     * @param supplier the underlying supplier
     * @return a supplier that performs value caching
     * @param <T> the type of object returned by the supplier
     */
    public static <T> Supplier<T> of(@NotNull Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier);
        return new MemoizingSupplier<>(supplier);
    }

    @Override
    public TData get() {
        if (!hasData) {
            cached = supplier.get();
            hasData = true;
        }

        return cached;
    }
}
