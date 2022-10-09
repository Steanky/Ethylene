package com.github.steanky.ethylene.mapper.signature.container;

import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArraySignature<T> extends ContainerSignatureBase<T[]> {
    public ArraySignature(@NotNull Token<T> componentType) {
        super(componentType, componentType.arrayType());
    }

    @Override
    public @NotNull Collection<TypedObject> objectData(@NotNull Object object) {
        int size = Array.getLength(object);
        return new AbstractCollection<>() {
            @Override
            public Iterator<TypedObject> iterator() {
                return new Iterator<>() {
                    private int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < size;
                    }

                    @Override
                    public TypedObject next() {
                        if (i++ == size) {
                            throw new NoSuchElementException();
                        }

                        return new TypedObject(null, ArraySignature.super.containerType, Array.get(object, i));
                    }
                };
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    @Override
    public @NotNull Object buildObject(@Nullable Object buildingObject, Object @NotNull [] args) {
        if (buildingObject == null) {
            return args;
        }

        System.arraycopy(args, 0, buildingObject, 0, args.length);
        return buildingObject;
    }

    @Override
    protected @NotNull Object makeBuildingObject(@NotNull ConfigContainer container) {
        return new Object[container.elementCollection().size()];
    }
}
