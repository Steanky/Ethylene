package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.signature.ScalarSignature;
import com.github.steanky.ethylene.mapper.type.Token;
import org.apache.commons.lang3.ClassUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;

public class BasicScalarSource implements ScalarSource {
    private static final int ELEMENT_TYPE_SIZE = ElementType.values().length;

    @SuppressWarnings("unchecked")
    private static final Map.Entry<ElementType, Set<ScalarSignature<?>>>[] EMPTY_MAP_ENTRY_ARRAY = new Map.Entry[0];

    private final TypeHinter typeHinter;
    private final Map<Class<?>, Map<ElementType, Set<ScalarSignature<?>>>> returnTypeMap;


    @SuppressWarnings("unchecked")
    public BasicScalarSource(@NotNull TypeHinter typeHinter, @NotNull Set<ScalarSignature<?>> signatures) {
        this.typeHinter = Objects.requireNonNull(typeHinter);

        if (signatures.isEmpty()) {
            this.returnTypeMap = Map.of();
        }
        else {
            //intermediateMap is only used to construct and validate the initial mappings
            //returnTypeMap is a precisely-sized WeakHashMap whose value maps are created using Map.ofEntries
            //(and therefore are more efficient than regular HashMaps)
            Map<Class<?>, Map<ElementType, Set<ScalarSignature<?>>>> intermediateMap = new HashMap<>(signatures.size());
            for (ScalarSignature<?> signature : signatures) {
                Token<?> objectType = signature.objectType();
                ElementType elementType = signature.elementType();

                intermediateMap.computeIfAbsent(objectType.rawType(), ignored -> new HashMap<>(ELEMENT_TYPE_SIZE))
                    .computeIfAbsent(elementType, ignored -> new HashSet<>(2)).add(signature);
            }

            this.returnTypeMap = new WeakHashMap<>(intermediateMap.size());
            for (Map.Entry<Class<?>, Map<ElementType, Set<ScalarSignature<?>>>> entry : intermediateMap.entrySet()) {
                Map<ElementType, Set<ScalarSignature<?>>> subMap = entry.getValue();
                Map.Entry<ElementType, Set<ScalarSignature<?>>>[] array = new Map.Entry[subMap.size()];

                int i = 0;
                for (Map.Entry<ElementType, Set<ScalarSignature<?>>> subEntry : subMap.entrySet()) {
                    array[i++] = Map.entry(subEntry.getKey(), Set.copyOf(subEntry.getValue()));
                }

                returnTypeMap.put(entry.getKey(), Map.ofEntries(array));
            }
        }
    }

    //necessary to use this method to avoid raw types
    private <T> ConfigElement createElement(ScalarSignature<T> scalarSignature, Object castTarget) {
        return scalarSignature.createElement(scalarSignature.objectType().cast(castTarget));
    }

    @Override
    public @NotNull ConfigElement makeElement(@Nullable Object data, @NotNull Token<?> upperBounds) {
        if (data == null) {
            //null assignable to upper bounds in all cases
            return ConfigPrimitive.NULL;
        }

        Class<?> dataClass = data.getClass();
        boolean assignable = upperBounds.isSuperclassOf(dataClass);
        if (ConfigPrimitive.isPrimitive(data) && assignable) {
            //simplest case, covers all standard primitives
            return ConfigPrimitive.of(data);
        }

        //walk the hierarchy until we find a matching signature
        Token<?> dataClassToken = Token.ofClass(dataClass);
        ElementType elementType = typeHinter.getHint(dataClassToken);
        for (Class<?> parent : dataClassToken.hierarchy(ClassUtils.Interfaces.INCLUDE)) {
            Map<ElementType, Set<ScalarSignature<?>>> typeMappings = returnTypeMap.get(parent);

            //signature found for this data type
            if (typeMappings != null) {
                Set<ScalarSignature<?>> signatures = typeMappings.get(elementType);

                //signature found for this element type
                if (signatures != null) {
                    for (ScalarSignature<?> signature : signatures) {
                        if (upperBounds.isSuperclassOf(signature.objectType())) {
                            return createElement(signature, data);
                        }
                    }
                }
            }
        }

        throw new IllegalArgumentException("Could not create scalar with upper bounds '" + upperBounds.getTypeName() +
            "', no matching signature with ElementType '" + elementType + "'");
    }

    @Override
    public @Nullable Object makeObject(@NotNull ConfigElement element, @NotNull Token<?> upperBounds) {
        if (element.isNull()) {
            return null;
        }

        ElementType elementType = element.type();
        if (elementType.isScalar()) {
            Object scalar = element.asScalar();
            if (upperBounds.isSuperclassOf(scalar.getClass())) {
                return scalar;
            }
        }

        return null;
    }
}
