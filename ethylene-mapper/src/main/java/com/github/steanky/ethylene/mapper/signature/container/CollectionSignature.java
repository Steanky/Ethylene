package com.github.steanky.ethylene.mapper.signature.container;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.util.ReflectionUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class CollectionSignature extends ContainerSignatureBase {
    private final Constructor<?> constructor;
    private final boolean parameterless;

    public CollectionSignature(@NotNull Type componentType, @NotNull Type collectionClass) {
        super(componentType, collectionClass);

        Class<?> rawClass = ReflectionUtils.rawType(collectionClass);
        Constructor<?> constructor = ConstructorUtils.getMatchingAccessibleConstructor(rawClass, int.class);
        if (constructor == null) {
            constructor = ConstructorUtils.getMatchingAccessibleConstructor(rawClass);
            if (constructor == null) {
                throw new MapperException("no suitable collection constructor found for '" + collectionClass + "'");
            }

            parameterless = true;
        }
        else {
            parameterless = false;
        }

        this.constructor = constructor;
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
        if(!element.isContainer()) {
            throw new MapperException("expected container");
        }

        return makeNewCollection(element.asContainer().entryCollection().size());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object buildObject(@Nullable Object buildingObject, Object @NotNull [] args) {
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
            if (parameterless) {
                return (Collection<Object>) constructor.newInstance();
            }

            return (Collection<Object>) constructor.newInstance(size);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MapperException(e);
        }
    }
}
