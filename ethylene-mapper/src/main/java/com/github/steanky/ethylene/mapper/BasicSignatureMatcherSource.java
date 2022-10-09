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
import org.apache.commons.lang3.ClassUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Basic implementation of {@link SignatureMatcher.Source}.
 */
public class BasicSignatureMatcherSource implements SignatureMatcher.Source {
    private static final Signature<?>[] EMPTY_SIGNATURE_ARRAY = new Signature[0];

    private final TypeHinter typeHinter;
    private final SignatureBuilder.Selector signatureSelector;

    private final Cache<Type, SignatureMatcher> signatureCache;
    private final Cache<Class<?>, Set<Signature<?>>> customSignatureCache;

    /**
     * Creates a new instance of this class.
     *
     * @param typeHinter the {@link TypeHinter} used to obtain information about types
     * @param signatureSelector the {@link SignatureBuilder.Selector} used to select signature builders for custom
     *                          objects
     * @param customSignatures a collection of custom signatures
     */
    public BasicSignatureMatcherSource(@NotNull TypeHinter typeHinter,
        @NotNull SignatureBuilder.Selector signatureSelector,
        @NotNull Collection<? extends Signature<?>> customSignatures) {
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.signatureSelector = Objects.requireNonNull(signatureSelector);

        this.signatureCache = Caffeine.newBuilder().weakKeys().build();
        this.customSignatureCache = Caffeine.newBuilder().weakKeys().initialCapacity(customSignatures.size()).build();

        registerCustomSignatures(customSignatures);
    }

    private void registerCustomSignatures(Collection<? extends Signature<?>> signatures) {
        for (Signature<?> signature : signatures) {
            customSignatureCache.get(signature.returnType().rawType(), ignored -> new HashSet<>(1))
                .add(signature);
        }
    }

    @Override
    public SignatureMatcher matcherFor(@NotNull Token<?> token) {
        //signatureCache is a weak cache, which means it uses identity comparisons
        //this is fine for all subclasses of Type that can be returned by tokens, but not for tokens themselves as they
        //are not expected to be cached in any way
        return signatureCache.get(token.get(), ignored -> {
            for (Class<?> superclass : token.hierarchy(ClassUtils.Interfaces.INCLUDE)) {
                Set<Signature<?>> signatures = customSignatureCache.getIfPresent(superclass);
                if (signatures != null) {
                    return new BasicSignatureMatcher(signatures.toArray(EMPTY_SIGNATURE_ARRAY), typeHinter);
                }
            }

            return switch (typeHinter.getHint(token)) {
                case LIST -> {
                    if (token.isArrayType()) {
                        Signature<?>[] arraySignature = new Signature[]{new ArraySignature<>(token.componentType())};
                        yield new BasicSignatureMatcher(arraySignature, typeHinter);
                    } else {
                        if (token.isSubclassOf(Collection.class)) {
                            Token<?> parameterized = token.parameterize(token.supertypeVariables(Collection.class));
                            Token<?>[] args = parameterized.actualTypeParameters();
                            Signature<?>[] collectionSignature = new Signature[]{new CollectionSignature<>(args[0], token)};

                            yield new BasicSignatureMatcher(collectionSignature, typeHinter);
                        } else if (token.isSubclassOf(Map.class)) {
                            Token<?> parameterized = token.parameterize(token.supertypeVariables(Map.class));
                            Token<?>[] args = parameterized.actualTypeParameters();

                            Signature<?>[] mapSignature = new Signature[]{new MapSignature<>(args[0], args[1], token)};
                            yield new BasicSignatureMatcher(mapSignature, typeHinter);
                        }
                    }

                    throw new MapperException("Unexpected container-like type '" + token.getTypeName() + "'");
                }
                case NODE ->
                    new BasicSignatureMatcher(signatureSelector.select(token).buildSignatures(token), typeHinter);
                case SCALAR -> null;
            };
        });
    }
}
