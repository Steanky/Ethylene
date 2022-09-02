package com.github.steanky.ethylene.core.mapper.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public class RegistrableCustomSignatureProvider implements CustomSignatureProvider {
    public RegistrableCustomSignatureProvider() {

    }

    @Override
    public Signature @Nullable [] provide(@NotNull Type type) {
        return new Signature[0];
    }


}
