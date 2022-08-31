package com.github.steanky.ethylene.core.mapper.signature.container;

import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.mapper.TypeHinter;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Iterator;

public abstract class ContainerSignatureBase implements Signature {
    protected final Entry<String, Type> entry;

    public ContainerSignatureBase(@NotNull Type componentType) {
        this.entry = Entry.of(null, componentType);
    }

    @Override
    public @NotNull Iterable<Entry<String, Type>> types() {
        return () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Entry<String, Type> next() {
                return entry;
            }
        };
    }

    @Override
    public boolean hasArgumentNames() {
        return false;
    }

    @Override
    public TypeHinter.Hint type() {
        return TypeHinter.Hint.CONTAINER_LIKE;
    }
}
