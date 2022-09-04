package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.mapper.Token;
import com.github.steanky.ethylene.core.mapper.TypeHinter;
import org.jetbrains.annotations.NotNull;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

public interface Signature {
    @NotNull Iterable<Entry<String, Type>> argumentTypes();

    Object makeObject(@NotNull Object[] args);

    boolean hasArgumentNames();

    int length(@NotNull ConfigElement element);

    @NotNull ElementType typeHint();

    @NotNull Type returnType();

    static @NotNull <T> Signature custom(@NotNull Token<T> returnType, @NotNull Function<? super Object[], ?
                extends T> constructor, Token<?> @NotNull ... parameters) {
        Collection<Entry<String, Type>> parameterEntries = new ArrayList<>(parameters.length);
        for (Token<?> token : parameters) {
            parameterEntries.add(Entry.of(null, token.get()));
        }

        return new CustomSignatureBase(parameterEntries, returnType.get(), false, constructor);
    }

    @SafeVarargs
    static @NotNull <T> Signature custom(@NotNull Token<T> returnType, @NotNull Function<? super Object[], ?
            extends T> constructor, Entry<String, Token<?>> @NotNull ... parameters) {
        Collection<Entry<String, Type>> parameterEntries = new ArrayList<>(parameters.length);
        for (Entry<String, Token<?>> entry : parameters) {
            parameterEntries.add(Entry.of(Objects.requireNonNull(entry.getFirst(), "entry name"),
                    Objects.requireNonNull(entry.getSecond(), "entry type").get()));
        }

        return new CustomSignatureBase(parameterEntries, returnType.get(), true, constructor);
    }
}
