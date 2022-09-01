package com.github.steanky.ethylene.core.mapper.signature.container;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public class ArraySignature extends ContainerSignatureBase {
    public ArraySignature(@NotNull Type componentType) {
        super(componentType, TypeUtils.genericArrayType(componentType));
    }

    @Override
    public Object makeObject(@NotNull Object[] args) {
        return args;
    }
}
