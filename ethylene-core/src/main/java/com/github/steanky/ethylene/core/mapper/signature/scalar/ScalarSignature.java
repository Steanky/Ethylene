package com.github.steanky.ethylene.core.mapper.signature.scalar;

import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.mapper.TypeHinter;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;

public class ScalarSignature implements Signature {
    private final Collection<Entry<String, Type>> entry;

    public ScalarSignature(@NotNull Type scalarType) {
        this.entry = Collections.singleton(Entry.of(null, scalarType));
    }

    @Override
    public @NotNull Iterable<Entry<String, Type>> types() {
        return entry;
    }

    @Override
    public Object makeObject(@NotNull Object[] args) {
        return args[0];
    }

    @Override
    public boolean hasArgumentNames() {
        return false;
    }

    @Override
    public TypeHinter.Hint type() {
        return TypeHinter.Hint.SCALAR;
    }
}
