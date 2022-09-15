package com.github.steanky.ethylene.mapper.signature.container;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.mapper.type.Token;
import com.github.steanky.ethylene.mapper.signature.Signature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Iterator;

public abstract class ContainerSignatureBase implements Signature {
    protected final Entry<String, Token<?>> entry;
    protected final Token<?> containerType;

    public ContainerSignatureBase(@NotNull Token<?> componentType, @NotNull Token<?> containerType) {
        this.entry = Entry.of(null, componentType);
        this.containerType = containerType;
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
                return Entry.of(entry.getFirst(), entry.getSecond().get());
            }
        };
    }

    @Override
    public @NotNull ConfigContainer initContainer(int sizeHint) {
        return new ArrayConfigList(sizeHint);
    }

    @Override
    public boolean hasBuildingObject() {
        return true;
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
    public int length(@Nullable ConfigElement configElement) {
        if (configElement == null || !configElement.isContainer()) {
            return -1;
        }

        return configElement.asContainer().elementCollection().size();
    }

    @Override
    public @NotNull ElementType typeHint() {
        return ElementType.LIST;
    }

    @Override
    public @NotNull Type returnType() {
        return containerType.get();
    }
}
