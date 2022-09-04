package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

@FunctionalInterface
public interface SignatureBuilder {
    @NotNull Signature @NotNull [] buildSignatures(@NotNull Type type, @NotNull ConfigElement element);
}
