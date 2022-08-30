package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.collection.ConfigList;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class CollectionTypeFactory extends TypeFactoryBase {
    private final Type parameterType;

    private final Constructor<?> sizeConstructor;

    public CollectionTypeFactory(@NotNull Class<?> collectionType, @NotNull Type parameterType) {
        this.parameterType = Objects.requireNonNull(parameterType);

        this.sizeConstructor = ConstructorUtils.getAccessibleConstructor(collectionType, int.class);
        if (this.sizeConstructor == null) {
            throw new MapperException("unable to find suitable constructor for collection " + collectionType);
        }
    }

    @Override
    public @NotNull Signature signature(@NotNull ConfigElement providedElement) {
        if (!providedElement.isList()) {
            throw new MapperException("expected ConfigList");
        }

        ConfigList list = providedElement.asList();
        SignatureElement[] signatureElements = new SignatureElement[list.size()];
        for (int i = 0; i < signatureElements.length; i++) {
            signatureElements[i] = new SignatureElement(parameterType, i);
        }

        return new Signature(0, signatureElements);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull Object make(@NotNull Signature signature, @NotNull ConfigElement providedElement,
            @NotNull Object... objects) {
        if (!providedElement.isList()) {
            throw new MapperException("expected ConfigList");
        }

        int listSize = providedElement.asList().size();
        validateLengths(signature.elements().length, listSize, objects.length);

        try {
            Collection<Object> collection = (Collection<Object>) sizeConstructor.newInstance(listSize);
            collection.addAll(Arrays.asList(objects));
            return collection;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MapperException(e);
        }
    }
}
