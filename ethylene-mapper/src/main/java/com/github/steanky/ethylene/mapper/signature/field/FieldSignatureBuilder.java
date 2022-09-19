package com.github.steanky.ethylene.mapper.signature.field;

import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

public class FieldSignatureBuilder implements SignatureBuilder {
    public static final FieldSignatureBuilder INSTANCE = new FieldSignatureBuilder();

    private FieldSignatureBuilder() {}

    @Override
    public @NotNull Signature @NotNull [] buildSignatures(@NotNull Token<?> type) {
        return new Signature[] {new FieldSignature(type)};
    }
}
