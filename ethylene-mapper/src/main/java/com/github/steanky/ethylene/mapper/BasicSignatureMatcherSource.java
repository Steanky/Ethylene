package com.github.steanky.ethylene.mapper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.mapper.signature.BasicSignatureMatcher;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.mapper.signature.SignatureMatcher;
import com.github.steanky.ethylene.mapper.signature.container.ArraySignature;
import com.github.steanky.ethylene.mapper.signature.container.CollectionSignature;
import com.github.steanky.ethylene.mapper.signature.container.MapSignature;
import com.github.steanky.ethylene.mapper.type.Token;
import com.github.steanky.ethylene.mapper.util.ReflectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;

public class BasicSignatureMatcherSource implements SignatureMatcher.Source {
    private static final Signature[] EMPTY_SIGNATURE_ARRAY = new Signature[0];

    private final TypeHinter typeHinter;
    private final SignatureBuilder.Selector signatureSelector;

    private final Cache<Type, SignatureMatcher> signatureCache;
    private final Cache<Class<?>, Set<Signature>> customSignatures;

    public BasicSignatureMatcherSource(@NotNull TypeHinter typeHinter,
            @NotNull SignatureBuilder.Selector signatureSelector, @NotNull Collection<Signature> customSignatures) {
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.signatureSelector = Objects.requireNonNull(signatureSelector);

        this.signatureCache = Caffeine.newBuilder().weakKeys().build();
        this.customSignatures = Caffeine.newBuilder().weakKeys().maximumSize(customSignatures.size()).build();

        registerCustomSignatures(customSignatures);
    }

    private void registerCustomSignatures(Collection<Signature> signatures) {
        for (Signature signature : signatures) {
            customSignatures.get(ReflectionUtils.rawType(signature.returnType()),
                    ignored -> new HashSet<>(2)).add(signature);
        }
    }

    @Override
    public SignatureMatcher matcherFor(@NotNull Type resolvedType, @Nullable ConfigElement element) {
        return signatureCache.get(resolvedType, type -> {
            Class<?> raw = ReflectionUtils.rawType(type);

            for (Class<?> superclass : ClassUtils.hierarchy(raw, ClassUtils.Interfaces.INCLUDE)) {
                Set<Signature> signatures = customSignatures.getIfPresent(superclass);
                if (signatures != null) {
                    return new BasicSignatureMatcher(signatures.toArray(EMPTY_SIGNATURE_ARRAY), typeHinter);
                }
            }

            return switch (typeHinter.getHint(type)) {
                case LIST -> {
                    if (TypeUtils.isArrayType(type)) {
                        Signature[] arraySignature = new Signature[] {
                                new ArraySignature(Token.of(TypeUtils.getArrayComponentType(type)))};
                        yield new BasicSignatureMatcher(arraySignature, typeHinter);
                    } else {
                        if (Collection.class.isAssignableFrom(raw)) {
                            Type[] types = ReflectionUtils.extractGenericTypeParameters(type, Collection.class);
                            Signature[] collectionSignature = new Signature[] {
                                    new CollectionSignature(Token.of(types[0]), Token.of(type))};

                            yield new BasicSignatureMatcher(collectionSignature, typeHinter);
                        } else if (Map.class.isAssignableFrom(raw)) {
                            Type[] types = ReflectionUtils.extractGenericTypeParameters(type, Map.class);
                            Signature[] mapSignature = new Signature[] {
                                    new MapSignature(Token.of(types[0]), Token.of(types[1]), Token.of(type))};
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
