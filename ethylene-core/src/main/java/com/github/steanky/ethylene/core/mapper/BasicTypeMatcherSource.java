package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.mapper.signature.BasicTypeSignatureMatcher;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import com.github.steanky.ethylene.core.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.core.mapper.signature.TypeSignatureMatcher;
import com.github.steanky.ethylene.core.mapper.signature.container.ArraySignature;
import com.github.steanky.ethylene.core.mapper.signature.container.CollectionSignature;
import com.github.steanky.ethylene.core.mapper.signature.container.MapSignature;
import com.github.steanky.ethylene.core.util.ReflectionUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class BasicTypeMatcherSource implements TypeSignatureMatcher.Source {
    private final TypeHinter typeHinter;
    private final TypeResolver typeResolver;
    private final TypeSignatureMatcher.Source customTypeMatcherSource;
    private final SignatureBuilder objectSignatureBuilder;
    private final boolean matchParameterNames;
    private final boolean matchParameterTypeHints;

    public BasicTypeMatcherSource(@NotNull TypeHinter typeHinter, @NotNull TypeResolver typeResolver,
            @NotNull TypeSignatureMatcher.Source customTypeMatcherSource,
            @NotNull SignatureBuilder objectSignatureBuilder, boolean matchParameterNames,
            boolean matchParameterTypeHints) {
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.typeResolver = Objects.requireNonNull(typeResolver);
        this.customTypeMatcherSource = Objects.requireNonNull(customTypeMatcherSource);
        this.objectSignatureBuilder = Objects.requireNonNull(objectSignatureBuilder);
        this.matchParameterNames = matchParameterNames;
        this.matchParameterTypeHints = matchParameterTypeHints;
    }

    @Override
    public TypeSignatureMatcher matcherFor(@NotNull Type type, @NotNull ConfigElement element) {
        Type resolvedType = typeResolver.resolveType(type, element);
        TypeSignatureMatcher customSignatureMatcher = customTypeMatcherSource.matcherFor(resolvedType, element);
        if (customSignatureMatcher != null) {
            return customSignatureMatcher;
        }

        return switch (typeHinter.getHint(resolvedType)) {
            case LIST -> {
                if (TypeUtils.isArrayType(resolvedType)) {
                    Signature[] arraySignature =
                            new Signature[] { new ArraySignature(TypeUtils.getArrayComponentType(resolvedType)) };
                    yield new BasicTypeSignatureMatcher(arraySignature, typeHinter, false, false);
                }
                else {
                    Class<?> rawResolved = TypeUtils.getRawType(resolvedType, null);

                    if (Collection.class.isAssignableFrom(rawResolved)) {
                        Type[] types = ReflectionUtils.extractGenericTypeParameters(resolvedType, Collection.class);
                        Signature[] collectionSignature = new Signature[] { new CollectionSignature(types[0], resolvedType) };
                        yield new BasicTypeSignatureMatcher(collectionSignature, typeHinter, false, false);
                    }
                    else if (Map.class.isAssignableFrom(rawResolved)) {
                        Type[] types = ReflectionUtils.extractGenericTypeParameters(resolvedType, Map.class);
                        Signature[] mapSignature = new Signature[] { new MapSignature(types[0], types[1], resolvedType) };
                        yield new BasicTypeSignatureMatcher(mapSignature, typeHinter, false, false);
                    }
                }

                throw new MapperException("unexpected container-like type '" + resolvedType.getTypeName() + "'");
            }
            case NODE -> new BasicTypeSignatureMatcher(objectSignatureBuilder.buildSignatures(resolvedType, element),
                    typeHinter, matchParameterNames, matchParameterTypeHints);
            case SCALAR -> null;
        };
    }
}
