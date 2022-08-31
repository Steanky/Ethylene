package com.github.steanky.ethylene.core.mapper.signature.container;

import com.github.steanky.ethylene.core.mapper.MapperException;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

public class CollectionSignature extends ContainerSignatureBase {
    private final Constructor<?> constructor;
    private final boolean parameterless;

    public CollectionSignature(@NotNull Type componentType, Class<?> collectionClass) {
        super(componentType);

        Constructor<?> constructor = ConstructorUtils.getMatchingAccessibleConstructor(collectionClass, int.class);
        if (constructor == null) {
            constructor = ConstructorUtils.getMatchingAccessibleConstructor(collectionClass);
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
    public Object makeObject(@NotNull Object[] args) {
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
            else {
                return (Collection<Object>) constructor.newInstance(size);
            }
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MapperException(e);
        }
    }
}
