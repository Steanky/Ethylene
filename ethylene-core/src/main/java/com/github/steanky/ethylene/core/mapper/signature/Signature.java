package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.mapper.MapperException;
import com.github.steanky.ethylene.core.mapper.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

public interface Signature {
    record TypedObject(@Nullable String name, @NotNull Type type, @NotNull Object value) {}

    @NotNull Iterable<Entry<String, Type>> argumentTypes();

    default @NotNull Collection<TypedObject> objectData(@NotNull Object object) {
        throw new MapperException("not implemented");
    }

    default boolean hasBuildingObject() {
        return false;
    }

    default @NotNull Object initBuildingObject(@NotNull ConfigElement element) {
        throw new IllegalStateException("unsupported operation");
    }

    Object buildObject(@Nullable Object buildingObject, Object @NotNull [] args);

    boolean matchesArgumentNames();

    default boolean matchesTypeHints() {
        return false;
    }

    int length(@Nullable ConfigElement element);

    @NotNull ElementType typeHint();

    @NotNull Type returnType();

    default int priority() {
        return 0;
    }

    static @NotNull <T> Signature custom(@NotNull Token<T> returnType, @NotNull Function<? super Object[], ?
                extends T> constructor, @NotNull Token<?> @NotNull ... parameters) {
        Collection<Entry<String, Type>> parameterEntries = new ArrayList<>(parameters.length);
        for (Token<?> token : parameters) {
            parameterEntries.add(Entry.of(null, token.get()));
        }

        return new CustomSignature(parameterEntries, returnType.get(), false, constructor);
    }

    @SafeVarargs
    static @NotNull <T> Signature custom(@NotNull Token<T> returnType, @NotNull Function<? super Object[], ?
            extends T> constructor, Entry<String, Token<?>> @NotNull ... parameters) {
        Collection<Entry<String, Type>> parameterEntries = new ArrayList<>(parameters.length);
        for (Entry<String, Token<?>> entry : parameters) {
            parameterEntries.add(Entry.of(Objects.requireNonNull(entry.getFirst(), "entry name"),
                    Objects.requireNonNull(entry.getSecond(), "entry type").get()));
        }

        return new CustomSignature(parameterEntries, returnType.get(), true, constructor);
    }
}
