package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ConfigPrimitive;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.signature.ScalarSignature;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Basic implementation of {@link ScalarSource}.
 */
public class BasicScalarSource implements ScalarSource {
    private static final int ELEMENT_TYPE_SIZE = ElementType.values().length;
    private static final ScalarSignature<?>[] EMPTY_SIGNATURE_ARRAY = new ScalarSignature[0];

    private final TypeHinter typeHinter;
    private final Map<Class<?>, Map<ElementType, ScalarSignature<?>[]>> returnTypeMap;

    /**
     * Creates a new instance of this class.
     *
     * @param typeHinter the {@link TypeHinter} used to obtain information about types
     * @param signatures a collection of custom {@link ScalarSignature} objects
     */
    @SuppressWarnings("unchecked")
    public BasicScalarSource(@NotNull TypeHinter typeHinter,
        @NotNull Collection<? extends ScalarSignature<?>> signatures) {
        this.typeHinter = Objects.requireNonNull(typeHinter);
        Objects.requireNonNull(signatures);

        if (signatures.isEmpty()) {
            this.returnTypeMap = Map.of();
        } else {
            //intermediateMap is only used to construct and validate the initial mappings
            Map<Class<?>, Map<ElementType, Set<ScalarSignature<?>>>> intermediateMap = new HashMap<>(signatures.size());
            for (ScalarSignature<?> signature : signatures) {
                Token<?> objectType = signature.objectType();
                ElementType elementType = signature.elementType();

                intermediateMap.computeIfAbsent(objectType.rawType(), ignored -> new HashMap<>(ELEMENT_TYPE_SIZE))
                    .computeIfAbsent(elementType, ignored -> new HashSet<>(2)).add(signature);
            }

            //returnTypeMap is a precisely-sized WeakHashMap whose value maps are created using Map.ofEntries
            //(and therefore are more efficient than regular HashMaps)
            //load factor 1 to prevent map resizing when elements are added (for space efficiency; hash collisions are
            //unlikely, the map should remain small)
            this.returnTypeMap = new WeakHashMap<>(intermediateMap.size(), 1F);
            for (Map.Entry<Class<?>, Map<ElementType, Set<ScalarSignature<?>>>> entry : intermediateMap.entrySet()) {
                Map<ElementType, Set<ScalarSignature<?>>> subMap = entry.getValue();
                Map.Entry<ElementType, ScalarSignature<?>[]>[] array = new Map.Entry[subMap.size()];

                int i = 0;
                for (Map.Entry<ElementType, Set<ScalarSignature<?>>> subEntry : subMap.entrySet()) {
                    ScalarSignature<?>[] signatureArray = subEntry.getValue().toArray(EMPTY_SIGNATURE_ARRAY);
                    Arrays.sort(signatureArray);
                    array[i++] = Map.entry(subEntry.getKey(), signatureArray);
                }

                returnTypeMap.put(entry.getKey(), Map.ofEntries(array));
            }
        }
    }

    //necessary to use this method to avoid raw types
    private <T> ConfigElement createElement(ScalarSignature<T> scalarSignature, Object castTarget) {
        return scalarSignature.createElement(scalarSignature.objectType().cast(castTarget));
    }

    private ScalarSignature<?> resolveSignature(Token<?> dataType, Token<?> upperBounds) {
        Class<?> raw = dataType.rawType();
        Map<ElementType, ScalarSignature<?>[]> typeMappings = returnTypeMap.get(raw);

        if (typeMappings != null) {
            ScalarSignature<?>[] signatures = typeMappings.get(typeHinter.getHint(dataType));

            if (signatures != null) {
                for (ScalarSignature<?> scalarSignature : signatures) {
                    if (upperBounds.isSuperclassOf(scalarSignature.objectType())) {
                        return scalarSignature;
                    }
                }

            }
        }

        throw new MapperException(
            "Could not locate signature with upper bounds '" + upperBounds.getTypeName() + "', no matching signature");
    }

    @Override
    public @NotNull ConfigElement makeElement(@Nullable Object data, @NotNull Token<?> upperBounds) {
        if (data == null) {
            //null assignable to upper bounds in all cases
            return ConfigPrimitive.NULL;
        }

        Class<?> dataClass = data.getClass();
        if (ConfigPrimitive.isPrimitive(data) && upperBounds.isSuperclassOf(dataClass)) {
            //simplest case, covers all standard primitives
            return ConfigPrimitive.of(data);
        }

        return createElement(resolveSignature(Token.ofType(dataClass), upperBounds), data);
    }

    @Override
    public @Nullable Object makeObject(@NotNull ConfigElement element, @NotNull Token<?> upperBounds) {
        if (element.isNull()) {
            //null is assignable to everything
            return null;
        }

        ElementType elementType = element.type();
        if (elementType.isScalar()) {
            Object scalar = element.asScalar();
            if (upperBounds.isSuperclassOf(scalar.getClass())) {
                //simple case: we can return the scalar's underlying value
                return scalar;
            }
        }

        return resolveSignature(typeHinter.getPreferredType(element, upperBounds), upperBounds).createScalar(element);
    }
}
