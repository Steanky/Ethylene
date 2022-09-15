package com.github.steanky.ethylene.mapper.signature.container;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.type.Token;
import com.github.steanky.ethylene.mapper.util.ReflectionUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class CollectionSignature extends ContainerSignatureBase {
    private record ConstructorInfo(boolean parameterless, Constructor<?> constructor) {}

    private Reference<ConstructorInfo> constructorReference = new SoftReference<>(null);

    public CollectionSignature(@NotNull Token<?> componentType, @NotNull Token<?> collectionClass) {
        super(componentType, collectionClass);
    }

    private ConstructorInfo resolveConstructor() {
        ConstructorInfo cached = constructorReference.get();
        if (cached != null) {
            return cached;
        }

        Class<?> rawClass = ReflectionUtils.rawType(super.containerType.get());
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
        constructorReference = new SoftReference<>(constructorInfo);
        return constructorInfo;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull Collection<TypedObject> objectData(@NotNull Object object) {
        Collection<Object> objectCollection = (Collection<Object>) object;

        return new AbstractCollection<>() {
            @Override
            public Iterator<TypedObject> iterator() {
                return new Iterator<>() {
                    private final Iterator<Object> collectionIterator = objectCollection.iterator();

                    @Override
                    public boolean hasNext() {
                        return collectionIterator.hasNext();
                    }

                    @Override
                    public TypedObject next() {
                        return new TypedObject(null, CollectionSignature.this.entry.getSecond(),
                                collectionIterator.next());
                    }
                };
            }

            @Override
            public int size() {
                return objectCollection.size();
            }
        };
    }

    @Override
    public @NotNull Object initBuildingObject(@NotNull ConfigElement element) {
        if (!element.isContainer()) {
            throw new MapperException("expected container");
        }

        return makeNewCollection(element.asContainer().entryCollection().size());
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull Object buildObject(@Nullable Object buildingObject, Object @NotNull [] args) {
        if (buildingObject != null) {
            Collection<Object> buildingCollection = (Collection<Object>) buildingObject;
            buildingCollection.addAll(Arrays.asList(args));
            return buildingCollection;
        }

        Collection<Object> collection = makeNewCollection(args.length);
        collection.addAll(Arrays.asList(args));
        return collection;
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> makeNewCollection(int size) {
        try {
            ConstructorInfo constructorInfo = resolveConstructor();
            if (constructorInfo.parameterless) {
                return (Collection<Object>) constructorInfo.constructor.newInstance();
            }

            return (Collection<Object>) constructorInfo.constructor.newInstance(size);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MapperException(e);
        }
    }
}
