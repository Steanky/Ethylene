package com.github.steanky.ethylene.mapper.signature.record;

import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link SignatureBuilder} implementation for {@link RecordSignature}. This class is singleton; its instance can be
 * obtained using {@link RecordSignatureBuilder#INSTANCE}.
 */
public class RecordSignatureBuilder implements SignatureBuilder {
    /**
     * The singleton instance of this object.
     */
    public static final RecordSignatureBuilder INSTANCE = new RecordSignatureBuilder();

    private RecordSignatureBuilder() {
    }

    @Override
    public @NotNull Signature<?> @NotNull [] buildSignatures(@NotNull Token<?> type) {
        return new Signature[]{new RecordSignature<>(type)};
    }
}
