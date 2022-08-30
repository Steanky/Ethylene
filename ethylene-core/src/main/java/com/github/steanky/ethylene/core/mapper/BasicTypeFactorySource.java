package com.github.steanky.ethylene.core.mapper;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

@SuppressWarnings("rawtypes")
public class BasicTypeFactorySource implements TypeFactory.Source {
    private static final TypeVariable<Class<Map>>[] MAP_VARIABLES = Map.class.getTypeParameters();

    private final TypeHinter typeHinter;
    private final TypeResolver resolver;
    private final boolean matchParameterNames;
    private final boolean matchParameterTypeHints;

    private final Map<Class<?>, TypeFactory> objectFactories;

    public BasicTypeFactorySource(@NotNull TypeHinter typeHinter, @NotNull TypeResolver resolver,
            boolean matchParameterNames,
            boolean matchParameterTypeHints) {
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.resolver = Objects.requireNonNull(resolver);
        this.matchParameterNames = matchParameterNames;
        this.matchParameterTypeHints = matchParameterTypeHints;
        this.objectFactories = new WeakHashMap<>();
    }

    @Override
    public @NotNull TypeFactory factory(@NotNull Type type) {
        Class<?> resolvedType = resolver.resolveType(type);

        return switch (typeHinter.getHint(type)) {
            case ARRAY_LIKE -> new ArrayTypeFactory(resolvedType.getComponentType());
            case OBJECT ->
                    objectFactories.computeIfAbsent(resolvedType, key -> new ConstructorTypeFactory(key, typeHinter,
                            matchParameterNames, matchParameterTypeHints));
            case COLLECTION_LIKE -> {
                Map<TypeVariable<?>, Type> typeVariables = TypeUtils.getTypeArguments(type, Collection.class);
                Type componentType = typeVariables.values().iterator().next();

                yield new CollectionTypeFactory(resolvedType, componentType);
            }
            case MAP_LIKE -> {
                Map<TypeVariable<?>, Type> typeVariables = TypeUtils.getTypeArguments(type, Map.class);
                Type keyType = typeVariables.get(MAP_VARIABLES[0]);
                Type valueType = typeVariables.get(MAP_VARIABLES[1]);

                yield new MapTypeFactory(resolvedType, keyType, valueType);
            }
            case SCALAR -> new ScalarTypeFactory(resolvedType);
        };
    }
}
