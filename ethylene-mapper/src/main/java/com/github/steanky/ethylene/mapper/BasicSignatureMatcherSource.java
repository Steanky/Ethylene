package com.github.steanky.ethylene.mapper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.steanky.ethylene.mapper.signature.BasicSignatureMatcher;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.mapper.signature.SignatureMatcher;
import com.github.steanky.ethylene.mapper.signature.container.ArraySignature;
import com.github.steanky.ethylene.mapper.signature.container.CollectionSignature;
import com.github.steanky.ethylene.mapper.signature.container.MapSignature;
import com.github.steanky.ethylene.mapper.type.Token;
import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.*;

public class BasicSignatureMatcherSource implements SignatureMatcher.Source {
    private static final Signature[] EMPTY_SIGNATURE_ARRAY = new Signature[0];

    private final TypeHinter typeHinter;
    private final SignatureBuilder.Selector signatureSelector;

    private final Cache<Type, SignatureMatcher> signatureCache;
    private final Cache<Class<?>, Set<Signature>> customSignatureCache;

    public BasicSignatureMatcherSource(@NotNull TypeHinter typeHinter,
            @NotNull SignatureBuilder.Selector signatureSelector, @NotNull Collection<Signature> customSignatures) {
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.signatureSelector = Objects.requireNonNull(signatureSelector);

        this.signatureCache = Caffeine.newBuilder().weakKeys().build();
        this.customSignatureCache = Caffeine.newBuilder().weakKeys().initialCapacity(customSignatures.size()).build();

        registerCustomSignatures(customSignatures);
    }

    private void registerCustomSignatures(Collection<Signature> signatures) {
        for (Signature signature : signatures) {
            customSignatureCache.get(ReflectionUtils.rawType(signature.returnType()), ignored ->
                    new HashSet<>(1)).add(signature);
        }
    }

    @Override
    public SignatureMatcher matcherFor(@NotNull Token<?> token) {
        return signatureCache.get(token.get(), ignored -> {
            Type type = token.get();

            for (Class<?> superclass : ClassUtils.hierarchy(token.rawType(), ClassUtils.Interfaces.INCLUDE)) {
                Set<Signature> signatures = customSignatureCache.getIfPresent(superclass);
                if (signatures != null) {
                    return new BasicSignatureMatcher(signatures.toArray(EMPTY_SIGNATURE_ARRAY), typeHinter);
                }
            }

            return switch (typeHinter.getHint(token)) {
                case LIST -> {
                    if (token.isArrayType()) {
                        Signature[] arraySignature = new Signature[] {new ArraySignature(token.componentType())};
                        yield new BasicSignatureMatcher(arraySignature, typeHinter);
                    } else {
                        if (token.assignable(Collection.class)) {
                            Type[] types = ReflectionUtils.extractGenericTypeParameters(type, Collection.class);
                            Signature[] collectionSignature = new Signature[] {new CollectionSignature(Token
                                    .ofType(types[0]), Token.ofType(type))};

                            yield new BasicSignatureMatcher(collectionSignature, typeHinter);
                        } else if (token.assignable(Map.class)) {
                            Type[] types = ReflectionUtils.extractGenericTypeParameters(type, Map.class);
                            Signature[] mapSignature = new Signature[] {new MapSignature(Token.ofType(types[0]),
                                    Token.ofType(types[1]), Token.ofType(type))};
                            yield new BasicSignatureMatcher(mapSignature, typeHinter);
                        }
                    }

                    throw new MapperException("Unexpected container-like type '" + type.getTypeName() + "'");
                }
                case NODE ->
                        new BasicSignatureMatcher(signatureSelector.select(type).buildSignatures(type), typeHinter);
                case SCALAR -> null;
            };
        });
    }
}
