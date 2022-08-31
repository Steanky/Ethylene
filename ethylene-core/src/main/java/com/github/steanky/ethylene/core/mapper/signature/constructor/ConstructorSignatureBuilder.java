package com.github.steanky.ethylene.core.mapper.signature.constructor;

import com.github.steanky.ethylene.core.mapper.MapperException;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import com.github.steanky.ethylene.core.mapper.signature.SignatureBuilder;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

public class ConstructorSignatureBuilder implements SignatureBuilder {

    @Override
    public @NotNull Signature[] buildSignatures(@NotNull Type type) {
        Class<?> rawType = TypeUtils.getRawType(type, null);
        if (rawType == null) {
            throw new MapperException("raw type was null for " + type);
        }

        Constructor<?>[] allConstructors = rawType.getConstructors();
        Signature[] specArray = new Signature[allConstructors.length];

        int j = 0;
        for (Constructor<?> constructor : allConstructors) {
            if (constructor.canAccess(null)) {
                specArray[j++] = new ConstructorSignature(constructor);
            }
        }

        if (j < specArray.length) {
            Signature[] resized = new Signature[j];
            System.arraycopy(specArray, 0, resized, 0, j);
            return resized;
        }

        return specArray;
    }
}
