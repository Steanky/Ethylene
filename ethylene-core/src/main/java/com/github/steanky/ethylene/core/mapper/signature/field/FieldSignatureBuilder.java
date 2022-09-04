package com.github.steanky.ethylene.core.mapper.signature.field;

import com.github.steanky.ethylene.core.mapper.signature.Signature;
import com.github.steanky.ethylene.core.mapper.signature.SignatureBuilder;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public class FieldSignatureBuilder implements SignatureBuilder {

    @Override
    public @NotNull Signature @NotNull [] buildSignatures(@NotNull Type type) {
        return new Signature[] { new FieldSignature(type) };
    }
}
