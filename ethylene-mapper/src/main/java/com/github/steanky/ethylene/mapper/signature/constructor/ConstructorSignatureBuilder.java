package com.github.steanky.ethylene.mapper.signature.constructor;

import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.annotation.Widen;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureBuilder;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

public class ConstructorSignatureBuilder implements SignatureBuilder {
    public static final ConstructorSignatureBuilder INSTANCE = new ConstructorSignatureBuilder();

    private ConstructorSignatureBuilder() {}

    @Override
    public @NotNull Signature @NotNull [] buildSignatures(@NotNull Type type) {
        Class<?> rawType = TypeUtils.getRawType(type, null);
        if (rawType == null) {
            throw new MapperException("raw type was null for " + type);
        }

        boolean widenAccess = rawType.isAnnotationPresent(Widen.class);
        Constructor<?>[] candidateConstructors = widenAccess ? rawType.getDeclaredConstructors() : rawType.getConstructors();
        Signature[] signatures = new Signature[candidateConstructors.length];
        int j = 0;
        for (Constructor<?> constructor : candidateConstructors) {
            if (widenAccess) {
                if (!constructor.trySetAccessible()) {
                    continue;
                }
            }

            signatures[j++] = new ConstructorSignature(constructor, type);
        }

        if (j < signatures.length) {
            Signature[] resized = new Signature[j];
            System.arraycopy(signatures, 0, resized, 0, j);
            return resized;
        }

        return signatures;
    }
}
