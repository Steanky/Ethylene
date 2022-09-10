package com.github.steanky.ethylene.mapper.signature.container;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.mapper.signature.Signature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public @NotNull ConfigContainer initContainer(int sizeHint) {
        return new ArrayConfigList(sizeHint);
    }

    @Override
    public boolean matchesArgumentNames() {
        return false;
    }

    @Override
    public boolean matchesTypeHints() {
        return true;
    }

    @Override
    public @NotNull ElementType typeHint() {
        return ElementType.LIST;
    }

    @Override
    public @NotNull Type returnType() {
        return containerType;
    }

    @Override
    public int length(@Nullable ConfigElement configElement) {
        if (configElement == null || !configElement.isContainer()) {
            return -1;
        }

        return configElement.asContainer().elementCollection().size();
    }

    @Override
    public boolean hasBuildingObject() {
        return true;
    }
}
