package com.github.steanky.ethylene.core.mapper.signature.container;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.mapper.MapperException;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;

public class MapSignature extends ContainerSignatureBase {
    private final boolean parameterless;
    private final Constructor<?> constructor;

    private Map<Object, Object> buildingMap;

    public MapSignature(@NotNull Type keyType, @NotNull Type valueType, @NotNull Type mapType) {
        super(makeMapComponentType(keyType, valueType), mapType);
        Class<?> mapClass = TypeUtils.getRawType(mapType, null);

        Class<?> rawClass = TypeUtils.getRawType(mapClass, null);
        Constructor<?> constructor = ConstructorUtils.getMatchingAccessibleConstructor(rawClass, int.class);
        if (constructor == null) {
            constructor = ConstructorUtils.getMatchingAccessibleConstructor(rawClass);
            if (constructor == null) {
                throw new MapperException("no suitable collection constructor found for '" + mapClass + "'");
            }

            parameterless = true;
        }
        else {
            parameterless = false;
        }

        this.constructor = constructor;
    }

    private static Type makeMapComponentType(Type keyType, Type valueType) {
        return TypeUtils.parameterize(Map.Entry.class, keyType, valueType);
    }

    @Override
    public void initBuildingObject(@NotNull ConfigElement element) {
        if(!element.isContainer()) {
            throw new MapperException("expected container");
        }

        this.buildingMap = getMap(element.asContainer().entryCollection().size());
    }

    @Override
    public @NotNull Object getBuildingObject() {
        if (buildingMap == null) {
            throw new MapperException("building object has not been initialized");
        }

        return buildingMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object buildObject(@NotNull Object[] args) {
        for (Object object : args) {
            Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>)object;
            buildingMap.put(entry.getKey(), entry.getValue());
        }

        return buildingMap;
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> getMap(int size) {
        try {
            return parameterless ? (Map<Object, Object>) constructor.newInstance() : (Map<Object, Object>) constructor
                    .newInstance(size);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MapperException(e);
        }
    }
}
