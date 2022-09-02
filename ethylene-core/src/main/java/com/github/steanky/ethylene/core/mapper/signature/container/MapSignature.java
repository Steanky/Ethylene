package com.github.steanky.ethylene.core.mapper.signature.container;

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

    @SuppressWarnings("unchecked")
    @Override
    public Object makeObject(@NotNull Object[] args) {
        try {
            Map<Object, Object> map = parameterless ? (Map<Object, Object>) constructor.newInstance() : (Map<Object,
                    Object>) constructor.newInstance(args.length);

            for (Object object : args) {
                Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>)object;
                map.put(entry.getKey(), entry.getValue());
            }

            return map;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MapperException(e);
        }

    }
}
