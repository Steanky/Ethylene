package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.mapper.signature.BasicSignatureMatcher;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import com.github.steanky.ethylene.core.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.core.mapper.signature.SignatureMatcher;
import com.github.steanky.ethylene.core.mapper.signature.container.ArraySignature;
import com.github.steanky.ethylene.core.mapper.signature.container.CollectionSignature;
import com.github.steanky.ethylene.core.mapper.signature.container.MapSignature;
import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class BasicTypeMatcherSource implements SignatureMatcher.Source {
    private final TypeHinter typeHinter;
    private final TypeResolver typeResolver;
    private final SignatureMatcher.Source customTypeMatcherSource;
    private final SignatureBuilder.Selector signatureSelector;

    private final Map<Type, SignatureMatcher> signatureCache;

    public BasicTypeMatcherSource(@NotNull TypeHinter typeHinter, @NotNull TypeResolver typeResolver,
            @NotNull SignatureMatcher.Source customTypeMatcherSource,
            @NotNull SignatureBuilder.Selector signatureSelector) {
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.typeResolver = Objects.requireNonNull(typeResolver);
        this.customTypeMatcherSource = Objects.requireNonNull(customTypeMatcherSource);
        this.signatureSelector = Objects.requireNonNull(signatureSelector);

        this.signatureCache = new WeakHashMap<>();
    }

    @Override
    public SignatureMatcher matcherFor(@NotNull Type resolvedType, @Nullable ConfigElement element) {
        SignatureMatcher customSignatureMatcher = customTypeMatcherSource.matcherFor(resolvedType, element);
        if (customSignatureMatcher != null) {
            return customSignatureMatcher;
        }

        return signatureCache.computeIfAbsent(resolvedType, t -> switch (typeHinter.getHint(t)) {
            case LIST -> {
                if (TypeUtils.isArrayType(t)) {
                    Signature[] arraySignature =
                            new Signature[] { new ArraySignature(TypeUtils.getArrayComponentType(t)) };
                    yield new BasicSignatureMatcher(arraySignature, typeHinter);
                }
                else {
                    Class<?> rawResolved = TypeUtils.getRawType(t, null);

                    if (Collection.class.isAssignableFrom(rawResolved)) {
                        Type[] types = ReflectionUtils.extractGenericTypeParameters(t, Collection.class);
                        Signature[] collectionSignature = new Signature[] { new CollectionSignature(types[0], t) };
                        yield new BasicSignatureMatcher(collectionSignature, typeHinter);
                    }
                    else if (Map.class.isAssignableFrom(rawResolved)) {
                        Type[] types = ReflectionUtils.extractGenericTypeParameters(t, Map.class);
                        Signature[] mapSignature = new Signature[] { new MapSignature(types[0], types[1], t) };
                        yield new BasicSignatureMatcher(mapSignature, typeHinter);
                    }
                }

                throw new MapperException("unexpected container-like type '" + t.getTypeName() + "'");
            }
            case NODE -> new BasicSignatureMatcher(signatureSelector.select(t).buildSignatures(t), typeHinter);
            case SCALAR -> null;
        });
    }
}
