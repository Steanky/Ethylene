package com.github.steanky.ethylene.core.mapper;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class BasicTypeFactorySource implements TypeFactory.Source {
    @SuppressWarnings("rawtypes")
    private static final TypeVariable<Class<Map>>[] MAP_VARIABLES = Map.class.getTypeParameters();

    private final TypeHinter typeHinter;
    private final TypeResolver resolver;
    private final boolean matchParameterNames;
    private final boolean matchParameterTypeHints;

    private final Map<Class<?>, TypeFactory> objectFactories;

    public BasicTypeFactorySource(@NotNull TypeHinter typeHinter, @NotNull TypeResolver resolver,
            boolean matchParameterNames, boolean matchParameterTypeHints) {
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.resolver = Objects.requireNonNull(resolver);
        this.matchParameterNames = matchParameterNames;
        this.matchParameterTypeHints = matchParameterTypeHints;
        this.objectFactories = new WeakHashMap<>();
    }

    @Override
    public @NotNull TypeFactory factory(@NotNull Type type) {
        return switch (typeHinter.getHint(type)) {
            case ARRAY_LIKE -> new ArrayTypeFactory(TypeUtils.getArrayComponentType(type));

            //TODO: allow users to register specific constructors to handle certain generic types
            case OBJECT ->
                    objectFactories.computeIfAbsent(resolver.resolveType(type), key -> new ConstructorTypeFactory(key, typeHinter,
                            matchParameterNames, matchParameterTypeHints));
            case COLLECTION_LIKE -> {
                Map<TypeVariable<?>, Type> typeVariables = TypeUtils.getTypeArguments(type, Collection.class);
                Type componentType = typeVariables.values().iterator().next();

                yield new CollectionTypeFactory(resolver.resolveType(type), componentType);
            }
            case MAP_LIKE -> {
                Map<TypeVariable<?>, Type> typeVariables = TypeUtils.getTypeArguments(type, Map.class);
                Type keyType = typeVariables.get(MAP_VARIABLES[0]);
                Type valueType = typeVariables.get(MAP_VARIABLES[1]);

                yield new MapTypeFactory(resolver.resolveType(type), keyType, valueType);
            }
            case SCALAR -> new ScalarTypeFactory(resolver.resolveType(type));
        };
    }
}
