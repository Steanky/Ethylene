package com.github.steanky.ethylene.core.mapper.signature.container;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.mapper.MapperException;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public class ArraySignature extends ContainerSignatureBase {
    private Object[] array;

    public ArraySignature(@NotNull Type componentType) {
        super(componentType, TypeUtils.genericArrayType(componentType));
    }

    @Override
    public void initBuildingObject(@NotNull ConfigElement element) {
        if(!element.isContainer()) {
            throw new MapperException("expected container");
        }

        this.array = new Object[element.asContainer().elementCollection().size()];
    }

    @Override
    public @NotNull Object getBuildingObject() {
        if (array == null) {
            throw new MapperException("building object has not been initialized");
        }

        return array;
    }

    @Override
    public Object buildObject(@NotNull Object[] args) {
        System.arraycopy(args, 0, array, 0, args.length);
        return array;
    }
}
