package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

public class MapTypeFactory extends TypeFactoryBase {
    private final Type keyType;
    private final Type valueType;

    private final Constructor<?> sizeConstructor;

    public MapTypeFactory(@NotNull Class<?> mapType, @NotNull Type keyType, @NotNull Type valueType) {
        this.keyType = Objects.requireNonNull(keyType);
        this.valueType = Objects.requireNonNull(valueType);

        this.sizeConstructor = ConstructorUtils.getAccessibleConstructor(mapType, int.class);
        if (sizeConstructor == null) {
            throw new MapperException("unable to find suitable constructor for map '" + mapType + "'");
        }
    }

    @Override
    public @NotNull Signature signature(@NotNull ConfigElement providedElement) {
        if (!providedElement.isList()) {
            throw new MapperException("expected ConfigList");
        }

        int size = providedElement.asList().size();
        Type entryType = TypeUtils.parameterize(Map.Entry.class, keyType, valueType);

        SignatureElement[] signatureElements = new SignatureElement[size];
        for (int i = 0; i < signatureElements.length; i++) {
            signatureElements[i] = new SignatureElement(entryType, i);
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

        validateLengths(signature.elements().length, providedElement.asList().size(), objects.length);

        try {
            Map<Object, Object> map = (Map<Object, Object>) sizeConstructor.newInstance(objects.length);
            for (Object object : objects) {
                Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>)object;
                map.put(entry.getKey(), entry.getValue());
            }

            return map;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MapperException(e);
        }
    }
}
