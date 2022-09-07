package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.mapper.BasicTypeResolver;
import com.github.steanky.ethylene.core.mapper.TypeResolver;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;

public class CustomSignatureBuilder implements SignatureBuilder {
    private static final Signature[] EMPTY_SIGNATURE_ARRAY = new Signature[0];

    private final BasicTypeResolver typeResolver;
    private final Map<String, Collection<Signature>> signatureMap;

    public CustomSignatureBuilder(@NotNull BasicTypeResolver typeResolver) {
        this.typeResolver = Objects.requireNonNull(typeResolver);
        this.signatureMap = new HashMap<>(8);
    }

    @Override
    public @NotNull Signature @NotNull [] buildSignatures(@NotNull Type type) {
        Type resolvedType = typeResolver.resolveType(type, null);
        Collection<Signature> signatureCollection = signatureMap.get(resolvedType.getTypeName());
        if (signatureCollection == null) {
            return EMPTY_SIGNATURE_ARRAY;
        }

        return signatureCollection.toArray(EMPTY_SIGNATURE_ARRAY);
    }

    public void registerCustomSignature(@NotNull Signature signature, @Nullable Class<?> superclass) {
        Type type = signature.returnType();

        if (superclass != null) {
            Class<?> rawReturnType = TypeUtils.getRawType(type, null);
            typeResolver.registerTypeImplementation(superclass, rawReturnType);
        }

        signatureMap.computeIfAbsent(type.getTypeName(), key -> new ArrayList<>(4)).add(signature);
    }
}
