package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.signature.ScalarSignature;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;

public class BasicScalarSource implements ScalarSource {
    private static final int ELEMENT_TYPE_SIZE = ElementType.values().length;

    @SuppressWarnings("unchecked")
    private static final Map.Entry<ElementType, ScalarSignature<?>>[] EMPTY_MAP_ENTRY_ARRAY = new Map.Entry[0];

    private final TypeHinter typeHinter;
    private final Map<Type, Map<ElementType, ScalarSignature<?>>> returnTypeMap;


    public BasicScalarSource(@NotNull TypeHinter typeHinter, @NotNull Set<ScalarSignature<?>> signatures) {
        this.typeHinter = Objects.requireNonNull(typeHinter);

        if (signatures.isEmpty()) {
            this.returnTypeMap = Map.of();
        }
        else {
            //intermediateMap is only used to construct and validate the initial mappings
            //returnTypeMap is a precisely-sized WeakHashMap whose value maps are created using Map.ofEntries
            //(and therefore are more efficient than regular HashMaps)
            Map<Type, Map<ElementType, ScalarSignature<?>>> intermediateMap = new HashMap<>(signatures.size());
            for (ScalarSignature<?> signature : signatures) {
                Token<?> objectType = signature.objectType();
                ElementType elementType = signature.elementType();

                if (intermediateMap.computeIfAbsent(signature.objectType().get(),
                    type -> new HashMap<>(ELEMENT_TYPE_SIZE)).putIfAbsent(elementType, signature) != null) {
                    throw new IllegalArgumentException("Tried to register more than one signature for object type '" +
                        objectType.getTypeName() + "' and element type '" + elementType + "'");
                }
            }

            this.returnTypeMap = new WeakHashMap<>(intermediateMap.size());
            for (Map.Entry<Type, Map<ElementType, ScalarSignature<?>>> entry : intermediateMap.entrySet()) {
                this.returnTypeMap.put(entry.getKey(), Map.ofEntries(entry.getValue().entrySet()
                    .toArray(EMPTY_MAP_ENTRY_ARRAY)));
            }
        }
    }

    private ScalarSignature<?> locateSignature(Token<?> token, ElementType desiredType) {
        Type type = token.get();
        Map<ElementType, ScalarSignature<?>> scalarSignature = returnTypeMap.get(type);

        if (scalarSignature == null) {
            throw new MapperException("No scalar signature found for data type '" + token.getTypeName() + "'");
        }

        ScalarSignature<?> desiredSignature = scalarSignature.get(desiredType);
        if (desiredSignature == null) {
            throw new MapperException("No scalar signature found for element type '" + desiredType + "'");
        }

        return desiredSignature;
    }

    //necessary to use this method to avoid raw types
    private <T> ConfigElement createElement(ScalarSignature<T> scalarSignature, Object castTarget) {
        return scalarSignature.createElement(scalarSignature.objectType().cast(castTarget));
    }

    @Override
    public @NotNull ConfigElement makeElement(@Nullable Object data, @NotNull Token<?> type) {
        ElementType dataType = data == null ? ElementType.SCALAR : typeHinter.getHint(Token.ofClass(data
            .getClass()));
        if (dataType == ElementType.SCALAR && ConfigPrimitive.isPrimitive(data)) {
            //simple case: String, primitive type, or primitive type wrapper
            return new ConfigPrimitive(data);
        }

        return createElement(locateSignature(type, dataType), data);
    }

    @Override
    public @Nullable Object makeObject(@NotNull ConfigElement element, @NotNull Token<?> type) {
        if (element.isScalar()) {
            Object object = element.asScalar();

            //handle the simple case: element is a scalar, and its underlying object is assignable to the type
            if (object == null || type.isSuperclassOf(object.getClass())) {
                return object;
            }
        }

        return locateSignature(type, element.type()).createScalar(element);
    }
}
