package com.github.steanky.ethylene.core.mapper.signature;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

@FunctionalInterface
public interface SignatureBuilder {
    @NotNull Signature[] buildSignatures(@NotNull Type type);
}
