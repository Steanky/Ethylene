package com.github.steanky.ethylene.mapper.signature.constructor;

import com.github.steanky.ethylene.mapper.annotation.Widen;
import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;

/**
 * {@link SignatureBuilder} implementation for {@link ConstructorSignature}. This class is singleton; its instance can
 * be obtained through {@link ConstructorSignatureBuilder#INSTANCE}.
 */
public class ConstructorSignatureBuilder implements SignatureBuilder {
    /**
     * The singleton instance of this class.
     */
    public static final ConstructorSignatureBuilder INSTANCE = new ConstructorSignatureBuilder();

    private ConstructorSignatureBuilder() {
    }

    @Override
    public @NotNull Signature<?> @NotNull [] buildSignatures(@NotNull Token<?> type) {
        Class<?> rawType = type.rawType();

        boolean widenAccess = rawType.isAnnotationPresent(Widen.class);
        Constructor<?>[] candidateConstructors =
            widenAccess ? rawType.getDeclaredConstructors() : rawType.getConstructors();
        Signature<?>[] signatures = new Signature[candidateConstructors.length];
        int j = 0;
        for (Constructor<?> constructor : candidateConstructors) {
            if (widenAccess) {
                if (!constructor.trySetAccessible()) {
                    continue;
                }
            }

            signatures[j++] = new ConstructorSignature<>(constructor, type);
        }

        if (j == 0) {
            return ReflectionUtils.EMPTY_SIGNATURE_ARRAY;
        }

        if (j < signatures.length) {
            Signature<?>[] resized = new Signature[j];
            System.arraycopy(signatures, 0, resized, 0, j);
            return resized;
        }

        return signatures;
    }
}
