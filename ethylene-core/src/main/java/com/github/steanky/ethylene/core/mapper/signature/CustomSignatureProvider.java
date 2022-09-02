package com.github.steanky.ethylene.core.mapper.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public interface CustomSignatureProvider {
    Signature @Nullable [] provide(@NotNull Type type);
}
