package com.github.steanky.ethylene.mapper.signature;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CompositeSignatureBuilder implements SignatureBuilder {
    private static final Signature[] EMPTY_SIGNATURE_ARRAY = new Signature[0];

    private final SignatureBuilder[] signatureBuilders;

    public CompositeSignatureBuilder(SignatureBuilder @NotNull ... signatureBuilders) {
        this.signatureBuilders = Objects.requireNonNull(signatureBuilders);
    }

    @Override
    public @NotNull Signature @NotNull [] buildSignatures(@NotNull Type type) {
        if (signatureBuilders.length == 0) {
            return EMPTY_SIGNATURE_ARRAY;
        }

        List<Signature> signatureList = new ArrayList<>(signatureBuilders.length * 2);
        for (SignatureBuilder builder : signatureBuilders) {
            signatureList.addAll(Arrays.asList(builder.buildSignatures(type)));
        }

        return signatureList.toArray(EMPTY_SIGNATURE_ARRAY);
    }
}
