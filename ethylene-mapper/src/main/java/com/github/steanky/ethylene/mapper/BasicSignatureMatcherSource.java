package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.mapper.signature.BasicSignatureMatcher;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.mapper.signature.SignatureMatcher;
import com.github.steanky.ethylene.mapper.signature.container.ArraySignature;
import com.github.steanky.ethylene.mapper.signature.container.CollectionSignature;
import com.github.steanky.ethylene.mapper.signature.container.MapSignature;
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

    private final Map<Type, SignatureMatcher> signatureCache;
    private final Map<Class<?>, Set<Signature>> customSignatures;

    public BasicSignatureMatcherSource(@NotNull TypeHinter typeHinter,
            @NotNull SignatureBuilder.Selector signatureSelector) {
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.signatureSelector = Objects.requireNonNull(signatureSelector);

        this.signatureCache = new WeakHashMap<>();
        this.customSignatures = new WeakHashMap<>();
    }

    @Override
    public SignatureMatcher matcherFor(@NotNull Type resolvedType, @Nullable ConfigElement element) {
        return signatureCache.computeIfAbsent(resolvedType, type -> {
            Class<?> raw = ReflectionUtils.rawType(type);

            for (Class<?> superclass : ClassUtils.hierarchy(raw, ClassUtils.Interfaces.INCLUDE)) {
                Set<Signature> signatures = customSignatures.get(superclass);
                if (signatures != null) {
                    return new BasicSignatureMatcher(signatures.toArray(EMPTY_SIGNATURE_ARRAY), typeHinter);
                }
            }

            return switch (typeHinter.getHint(type)) {
                case LIST -> {
                    if (TypeUtils.isArrayType(type)) {
                        Signature[] arraySignature = new Signature[] {
                                new ArraySignature(TypeUtils.getArrayComponentType(type))};
                        yield new BasicSignatureMatcher(arraySignature, typeHinter);
                    } else {
                        if (Collection.class.isAssignableFrom(raw)) {
                            Type[] types = ReflectionUtils.extractGenericTypeParameters(type, Collection.class);
                            Signature[] collectionSignature = new Signature[] {new CollectionSignature(types[0], type)};
                            yield new BasicSignatureMatcher(collectionSignature, typeHinter);
                        } else if (Map.class.isAssignableFrom(raw)) {
                            Type[] types = ReflectionUtils.extractGenericTypeParameters(type, Map.class);
                            Signature[] mapSignature = new Signature[] {new MapSignature(types[0], types[1], type)};
                            yield new BasicSignatureMatcher(mapSignature, typeHinter);
                        }
                    }

                    throw new MapperException("unexpected container-like type '" + type.getTypeName() + "'");
                }
                case NODE ->
                        new BasicSignatureMatcher(signatureSelector.select(type).buildSignatures(type), typeHinter);
                case SCALAR -> null;
            };
        });
    }

    public void registerCustomSignature(@NotNull Signature signature) {
        customSignatures.computeIfAbsent(ReflectionUtils.rawType(signature.returnType()), ignored -> new HashSet<>(2))
                .add(signature);
    }
}
