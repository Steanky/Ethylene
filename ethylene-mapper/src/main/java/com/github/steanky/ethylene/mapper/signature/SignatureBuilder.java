package com.github.steanky.ethylene.mapper.signature;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

@FunctionalInterface
public interface SignatureBuilder {
    @NotNull Signature @NotNull [] buildSignatures(@NotNull Type type);

    interface Selector {
        @NotNull SignatureBuilder select(@NotNull Type type);

        void registerSignaturePreference(@NotNull Class<?> type, @NotNull SignatureBuilder signatureBuilder);
    }
}
