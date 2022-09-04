package com.github.steanky.ethylene.core.mapper.signature.field;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import com.github.steanky.ethylene.core.mapper.signature.SignatureBuilder;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public class FieldSignatureBuilder implements SignatureBuilder {

    @Override
    public @NotNull Signature @NotNull [] buildSignatures(@NotNull Type type, @NotNull ConfigElement element) {
        return new Signature[] { new FieldSignature(type) };
    }
}
