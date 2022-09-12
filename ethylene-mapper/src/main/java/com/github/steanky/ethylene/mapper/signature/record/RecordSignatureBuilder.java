package com.github.steanky.ethylene.mapper.signature.record;

import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureBuilder;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public class RecordSignatureBuilder implements SignatureBuilder {
    public static final RecordSignatureBuilder INSTANCE = new RecordSignatureBuilder();

    private RecordSignatureBuilder() {}

    @Override
    public @NotNull Signature @NotNull [] buildSignatures(@NotNull Type type) {
        return new Signature[] {new RecordSignature(type)};
    }
}
