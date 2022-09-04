package com.github.steanky.ethylene.core.mapper.signature;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

@FunctionalInterface
public interface SignatureBuilder {
    @NotNull Signature @NotNull [] buildSignatures(@NotNull Type type);

    @FunctionalInterface
    interface Selector {
        @NotNull SignatureBuilder select(@NotNull Type type);
    }
}
