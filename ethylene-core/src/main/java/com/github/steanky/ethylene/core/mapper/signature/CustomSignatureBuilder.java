package com.github.steanky.ethylene.core.mapper.signature;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.*;

public class CustomSignatureBuilder implements SignatureBuilder {
    private static final Signature[] EMPTY_SIGNATURE_ARRAY = new Signature[0];

    private final Map<String, Collection<Signature>> signatureMap;

    public CustomSignatureBuilder() {
        this.signatureMap = new HashMap<>(8);
    }

    @Override
    public @NotNull Signature @NotNull [] buildSignatures(@NotNull Type type) {
        Collection<Signature> signatureCollection = signatureMap.get(type.getTypeName());
        if (signatureCollection == null) {
            return EMPTY_SIGNATURE_ARRAY;
        }

        return signatureCollection.toArray(EMPTY_SIGNATURE_ARRAY);
    }

    public void registerCustomSignature(@NotNull Signature signature) {
        Type type = signature.returnType();
        signatureMap.computeIfAbsent(type.getTypeName(), key -> new ArrayList<>(4)).add(signature);
    }
}
