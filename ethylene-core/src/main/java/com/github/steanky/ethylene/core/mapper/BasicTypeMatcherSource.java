package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.mapper.signature.BasicTypeSignatureMatcher;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import com.github.steanky.ethylene.core.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.core.mapper.signature.TypeSignatureMatcher;
import com.github.steanky.ethylene.core.mapper.signature.container.ArraySignature;
import com.github.steanky.ethylene.core.mapper.signature.container.CollectionSignature;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("rawtypes")
public class BasicTypeMatcherSource implements TypeSignatureMatcher.Source {
    private static final TypeVariable<Class<Collection>>[] COLLECTION_TYPE_VARIABLES =
            Collection.class.getTypeParameters();

    private static final TypeVariable<Class<Map>>[] MAP_TYPE_VARIABLES = Map.class.getTypeParameters();


    private final TypeHinter typeHinter;
    private final TypeResolver resolver;
    private final SignatureBuilder objectSignatureBuilder;
    private final boolean matchParameterNames;
    private final boolean matchParameterTypeHints;

    public BasicTypeMatcherSource(@NotNull TypeHinter typeHinter, @NotNull TypeResolver typeResolver,
            @NotNull SignatureBuilder objectSignatureBuilder, boolean matchParameterNames,
            boolean matchParameterTypeHints) {
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.resolver = Objects.requireNonNull(typeResolver);
        this.objectSignatureBuilder = Objects.requireNonNull(objectSignatureBuilder);
        this.matchParameterNames = matchParameterNames;
        this.matchParameterTypeHints = matchParameterTypeHints;
    }

    @Override
    public TypeSignatureMatcher matcherFor(@NotNull Type type) {
        TypeHinter.Hint hint = typeHinter.getHint(type);

        return switch (hint) {
            case CONTAINER_LIKE -> {
                Class<?> resolvedType = resolver.resolveType(type);
                if (resolvedType.isArray()) {
                    Signature[] arraySignature = new Signature[] { new ArraySignature(resolvedType.getComponentType()) };
                    yield new BasicTypeSignatureMatcher(arraySignature, typeHinter, false, false);
                }
                else if (Collection.class.isAssignableFrom(resolvedType)) {
                    Type collectionType = TypeUtils.getTypeArguments(type, Collection.class)
                            .get(COLLECTION_TYPE_VARIABLES[0]);
                    if (collectionType instanceof TypeVariable<?>) {
                        collectionType = Object.class;
                    }

                    Signature[] collectionSignature = new Signature[] { new CollectionSignature(collectionType,
                            resolvedType) };
                    yield new BasicTypeSignatureMatcher(collectionSignature, typeHinter, false, false);
                }
                else if (Map.class.isAssignableFrom(resolvedType)) {
                    throw new UnsupportedOperationException("not implemented");
                }

                throw new MapperException("unexpected collection-like type '" + type.getTypeName() + "'");
            }
            case OBJECT_LIKE -> new BasicTypeSignatureMatcher(objectSignatureBuilder.buildSignatures(type), typeHinter,
                    matchParameterNames, matchParameterTypeHints);
            case SCALAR -> null;
        };
    }
}