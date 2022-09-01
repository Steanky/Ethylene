package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.mapper.TypeHinter;
import org.jetbrains.annotations.NotNull;
import java.lang.reflect.Type;

public interface Signature {
    @NotNull Iterable<Entry<String, Type>> argumentTypes();

    Object makeObject(@NotNull Object[] args);

    boolean hasArgumentNames();

    int length(@NotNull ConfigElement element);

    TypeHinter.Hint typeHint();

    @NotNull Type returnType();
}
