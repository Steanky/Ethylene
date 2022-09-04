package com.github.steanky.ethylene.core.mapper.signature.container;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.mapper.MapperException;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public class ArraySignature extends ContainerSignatureBase {
    public ArraySignature(@NotNull Type componentType) {
        super(componentType, TypeUtils.genericArrayType(componentType));
    }

    @Override
    public @NotNull Object initBuildingObject(@NotNull ConfigElement element) {
        if(!element.isContainer()) {
            throw new MapperException("expected container");
        }

        return new Object[element.asContainer().elementCollection().size()];
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    @Override
    public Object buildObject(@Nullable Object buildingObject, @NotNull Object[] args) {
        if (buildingObject == null) {
            return args;
        }

        System.arraycopy(args, 0, buildingObject, 0, args.length);
        return buildingObject;
    }
}
