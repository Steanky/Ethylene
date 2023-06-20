package com.github.steanky.ethylene.mapper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.steanky.ethylene.mapper.internal.ReflectionUtils;
import com.github.steanky.ethylene.mapper.signature.BasicSignatureMatcher;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.mapper.signature.SignatureMatcher;
import com.github.steanky.ethylene.mapper.signature.container.ArraySignature;
import com.github.steanky.ethylene.mapper.signature.container.CollectionSignature;
import com.github.steanky.ethylene.mapper.signature.container.MapSignature;
import com.github.steanky.ethylene.mapper.type.Token;
import com.github.steanky.ethylene.mapper.type.TypeVariableMap;
import org.apache.commons.lang3.ClassUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * Basic implementation of {@link SignatureMatcher.Source}.
 */
public class BasicSignatureMatcherSource implements SignatureMatcher.Source {
    private final TypeHinter typeHinter;
    private final SignatureBuilder.Selector signatureSelector;
    private final boolean matchLength;

    private final Cache<Type, SignatureMatcher> signatureCache;
    private final Cache<Class<?>, Set<Signature<?>>> customSignatureCache;

    /**
     * Creates a new instance of this class.
     *
     * @param typeHinter        the {@link TypeHinter} used to obtain information about types
     * @param signatureSelector the {@link SignatureBuilder.Selector} used to select signature builders for custom
     *                          objects
     * @param customSignatures  a collection of custom signatures
     * @param matchLength       true if the size of the data provided to create objects need not match the signature
     *                          length exactly
     */
    public BasicSignatureMatcherSource(@NotNull TypeHinter typeHinter,
        @NotNull SignatureBuilder.Selector signatureSelector,
        @NotNull Collection<? extends Signature<?>> customSignatures, boolean matchLength) {
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.signatureSelector = Objects.requireNonNull(signatureSelector);
        this.matchLength = matchLength;

        this.signatureCache = Caffeine.newBuilder().weakKeys().build();
        this.customSignatureCache = Caffeine.newBuilder().weakKeys().initialCapacity(customSignatures.size()).build();

        registerCustomSignatures(customSignatures);
    }

    private void registerCustomSignatures(Collection<? extends Signature<?>> signatures) {
        for (Signature<?> signature : signatures) {
            customSignatureCache.get(signature.returnType().rawType(), ignored -> new HashSet<>(1)).add(signature);
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
                    return new BasicSignatureMatcher(signatures.toArray(ReflectionUtils.EMPTY_SIGNATURE_ARRAY),
                        typeHinter, matchLength);
                }
            }

            return switch (typeHinter.getHint(token)) {
                case LIST -> {
                    if (token.isArrayType()) {
                        Signature<?>[] arraySignature = new Signature[]{new ArraySignature<>(token.componentType())};
                        yield new BasicSignatureMatcher(arraySignature, typeHinter, matchLength);
                    } else {
                        if (token.isSubclassOf(Collection.class)) {
                            TypeVariableMap mappings = token.supertypeVariables(Collection.class);

                            Token<?> key = Token.ofType(Collection.class.getTypeParameters()[0]);
                            TypeVariable<?> variable = (TypeVariable<?>)key.get();

                            Token<?> componentType = mappings.get(variable);
                            if (componentType == null) {
                                throw new MapperException("Unexpected type variable " + variable);
                            }

                            Signature<?>[] collectionSignature =
                                new Signature[]{new CollectionSignature<>(componentType, token)};

                            yield new BasicSignatureMatcher(collectionSignature, typeHinter, matchLength);
                        } else if (token.isSubclassOf(Map.class)) {
                            TypeVariableMap mappings = token.supertypeVariables(Map.class);

                            TypeVariable<?>[] typeVariables = Map.class.getTypeParameters();
                            Token<?> keyKey = Token.ofType(typeVariables[0]);
                            Token<?> valueKey = Token.ofType(typeVariables[1]);

                            Token<?> keyType = mappings.get((TypeVariable<?>)keyKey.get());
                            Token<?> valueType = mappings.get((TypeVariable<?>)valueKey.get());
                            if (keyType == null || valueType == null) {
                                throw new MapperException("Unexpected type variables " + Arrays.toString(typeVariables));
                            }

                            Signature<?>[] mapSignature = new Signature[]{new MapSignature<>(keyType, valueType, token)};
                            yield new BasicSignatureMatcher(mapSignature, typeHinter, matchLength);
                        }
                    }

                    throw new MapperException("Unexpected container-like type '" + token.getTypeName() + "'");
                }
                case NODE ->
                    new BasicSignatureMatcher(signatureSelector.select(token).buildSignatures(token), typeHinter,
                        matchLength);
                case SCALAR -> null;
            };
        });
    }
}
