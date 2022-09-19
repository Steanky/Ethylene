package com.github.steanky.ethylene.mapper.signature.constructor;

import com.github.steanky.ethylene.mapper.annotation.Widen;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;

public class ConstructorSignatureBuilder implements SignatureBuilder {
    public static final ConstructorSignatureBuilder INSTANCE = new ConstructorSignatureBuilder();
    private static final Signature[] EMPTY_SIGNATURE_ARRAY = new Signature[0];

    private ConstructorSignatureBuilder() {
    }

    @Override
    public @NotNull Signature @NotNull [] buildSignatures(@NotNull Token<?> type) {
        Class<?> rawType = type.rawType();

        boolean widenAccess = rawType.isAnnotationPresent(Widen.class);
        Constructor<?>[] candidateConstructors =
            widenAccess ? rawType.getDeclaredConstructors() : rawType.getConstructors();
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

        if (j == 0) {
            return EMPTY_SIGNATURE_ARRAY;
        }

        if (j < signatures.length) {
            Signature[] resized = new Signature[j];
            System.arraycopy(signatures, 0, resized, 0, j);
            return resized;
        }

        return signatures;
    }
}
