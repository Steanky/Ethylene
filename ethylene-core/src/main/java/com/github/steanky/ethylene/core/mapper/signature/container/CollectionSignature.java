package com.github.steanky.ethylene.core.mapper.signature.container;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.mapper.MapperException;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

public class CollectionSignature extends ContainerSignatureBase {
    private final Constructor<?> constructor;
    private final boolean parameterless;

    public CollectionSignature(@NotNull Type componentType, @NotNull Type collectionClass) {
        super(componentType, collectionClass);

        Class<?> rawClass = TypeUtils.getRawType(collectionClass, null);
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

    @Override
    public @NotNull Object initBuildingObject(@NotNull ConfigElement element) {
        if(!element.isContainer()) {
            throw new MapperException("expected container");
        }

        return makeNewCollection(element.asContainer().entryCollection().size());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object buildObject(@Nullable Object buildingObject, @NotNull Object[] args) {
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
