package com.github.steanky.ethylene.core.mapper.signature.container;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.mapper.MapperException;
import com.github.steanky.ethylene.core.mapper.TypeHinter;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Objects;

public abstract class ContainerSignatureBase implements Signature {
    protected final Entry<String, Type> entry;
    protected final Type containerType;

    public ContainerSignatureBase(@NotNull Type componentType, @NotNull Type containerType) {
        this.entry = Entry.of(null, componentType);
        this.containerType = Objects.requireNonNull(containerType);
    }

    @Override
    public @NotNull Iterable<Entry<String, Type>> argumentTypes() {
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
    public TypeHinter.Hint typeHint() {
        return TypeHinter.Hint.CONTAINER_LIKE;
    }

    @Override
    public @NotNull Type returnType() {
        return containerType;
    }

    @Override
    public int length(@NotNull ConfigElement configElement) {
        if (configElement.isContainer()) {
            return configElement.asContainer().elementCollection().size();
        }

        throw new MapperException("cannot compute the length of a container for non-container ConfigElement");
    }
}
