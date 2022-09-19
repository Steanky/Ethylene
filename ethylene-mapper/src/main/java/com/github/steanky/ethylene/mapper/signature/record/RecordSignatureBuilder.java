package com.github.steanky.ethylene.mapper.signature.record;

import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

public class RecordSignatureBuilder implements SignatureBuilder {
    public static final RecordSignatureBuilder INSTANCE = new RecordSignatureBuilder();

    private RecordSignatureBuilder() {
    }

    @Override
    public @NotNull Signature @NotNull [] buildSignatures(@NotNull Token<?> type) {
        return new Signature[]{new RecordSignature(type)};
    }
}
