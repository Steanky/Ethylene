package com.github.steanky.ethylene.mapper.signature.field;

import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

/**
 * {@link SignatureBuilder} implementation for {@link FieldSignature}. This class is singleton; its instance may be
 * obtained through {@link FieldSignatureBuilder#INSTANCE}.
 */
public class FieldSignatureBuilder implements SignatureBuilder {
    /**
     * The singleton instance of this class.
     */
    public static final FieldSignatureBuilder INSTANCE = new FieldSignatureBuilder();

    private FieldSignatureBuilder() {
    }

    @Override
    public @NotNull Signature<?> @NotNull [] buildSignatures(@NotNull Token<?> type) {
        return new Signature[]{new FieldSignature<>(type)};
    }
}
