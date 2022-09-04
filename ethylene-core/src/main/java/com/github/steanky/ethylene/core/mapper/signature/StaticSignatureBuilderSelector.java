package com.github.steanky.ethylene.core.mapper.signature;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Objects;

public class StaticSignatureBuilderSelector implements SignatureBuilder.Selector {
    private final SignatureBuilder builder;

    public StaticSignatureBuilderSelector(@NotNull SignatureBuilder builder) {
        this.builder = Objects.requireNonNull(builder);
    }

    @Override
    public @NotNull SignatureBuilder select(@NotNull Type type) {
        return builder;
    }
}
