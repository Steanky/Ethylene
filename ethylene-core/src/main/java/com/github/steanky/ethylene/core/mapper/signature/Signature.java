package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
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

    @NotNull Collection<TypedObject> objectData(@NotNull Object object);

    @NotNull ConfigContainer initContainer(int sizeHint);

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
}
