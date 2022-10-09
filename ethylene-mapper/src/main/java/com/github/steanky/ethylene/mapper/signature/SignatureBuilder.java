package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface SignatureBuilder {
    @NotNull Signature<?> @NotNull [] buildSignatures(@NotNull Token<?> type);

    @FunctionalInterface
    interface Selector {
        @NotNull SignatureBuilder select(@NotNull Token<?> type);
    }
}
