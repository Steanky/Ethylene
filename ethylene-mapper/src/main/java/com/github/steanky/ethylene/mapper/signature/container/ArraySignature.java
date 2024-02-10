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

/**
 * Signature implementation for all array types.
 *
 * @param <T> the component type of the array
 */
public class ArraySignature<T> extends ContainerSignatureBase<T[]> {
    /**
     * Creates a new instance of this class.
     *
     * @param componentType the component type of the array
     */
    public ArraySignature(@NotNull Token<T> componentType) {
        super(componentType, componentType.arrayType());
    }

    @Override
    public @NotNull Collection<TypedObject> objectData(@NotNull T @NotNull [] object) {
        int size = object.length;

        return new AbstractCollection<>() {
            @Override
            public @NotNull Iterator<TypedObject> iterator() {
                return new Iterator<>() {
                    private int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < size;
                    }

                    @Override
                    public TypedObject next() {
                        if (i >= size) {
                            throw new NoSuchElementException();
                        }

                        return new TypedObject(null, ArraySignature.super.entry.getValue().type(), object[i++]);
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
            buildingObject = Array.newInstance(containerType.componentType().rawType(), args.length);
        }

        if (containerType.componentType().rawType().isPrimitive()) {
            //manual arraycopy when component type is primitive, we must unbox as args will be boxed
            for (int i = 0; i < args.length; i++) {
                Array.set(buildingObject, i, args[i]);
            }

            return buildingObject;
        }

        //in other cases, we can do a likely-faster arraycopy
        System.arraycopy(args, 0, buildingObject, 0, args.length);
        return buildingObject;
    }

    @Override
    protected @NotNull Object makeBuildingObject(@NotNull ConfigContainer container) {
        return Array.newInstance(containerType.componentType().rawType(), container.elementCollection().size());
    }
}
