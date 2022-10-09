package com.github.steanky.ethylene.mapper.signature.container;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.PrioritizedBase;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.type.Token;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Map;

public abstract class ContainerSignatureBase<T> extends PrioritizedBase implements Signature<T> {
    protected final Map.Entry<String, Token<?>> entry;
    protected final Token<T> containerType;
    protected Reference<ConstructorInfo> constructorInfoReference = new SoftReference<>(null);

    public ContainerSignatureBase(@NotNull Token<?> componentType, @NotNull Token<T> containerType) {
        super(0);
        this.entry = Entry.of(null, componentType);
        this.containerType = containerType;
    }

    protected @NotNull ConstructorInfo resolveConstructor() {
        ConstructorInfo cached = constructorInfoReference.get();
        if (cached != null) {
            return cached;
        }

        Class<?> rawClass = containerType.rawType();
        Constructor<?> constructor = ConstructorUtils.getMatchingAccessibleConstructor(rawClass, int.class);

        boolean parameterless;
        if (constructor == null) {
            constructor = ConstructorUtils.getMatchingAccessibleConstructor(rawClass);

            if (constructor == null) {
                throw new MapperException("No suitable collection constructor found for '" + rawClass + "'");
            }

            parameterless = true;
        } else {
            parameterless = false;
        }

        ConstructorInfo constructorInfo = new ConstructorInfo(parameterless, constructor);
        constructorInfoReference = new SoftReference<>(constructorInfo);
        return constructorInfo;
    }

    @Override
    public @NotNull Iterable<Map.Entry<String, Token<?>>> argumentTypes() {
        return () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Map.Entry<String, Token<?>> next() {
                return entry;
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
    public @NotNull Object initBuildingObject(@NotNull ConfigElement element) {
        if (!element.isContainer()) {
            throw new MapperException("Expected container, got '" + element.type() + "'");
        }

        return makeBuildingObject(element.asContainer());
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
    public @NotNull Token<T> returnType() {
        return containerType;
    }

    protected abstract @NotNull Object makeBuildingObject(@NotNull ConfigContainer container);

    protected record ConstructorInfo(boolean parameterless, Constructor<?> constructor) {
    }
}
