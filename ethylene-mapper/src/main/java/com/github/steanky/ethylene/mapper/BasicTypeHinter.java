package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Basic implementation of {@link TypeHinter}.
 */
public class BasicTypeHinter implements TypeHinter {
    private static final Token<?> ARRAY_LIST = Token.ofClass(ArrayList.class).parameterize(Object.class);

    private final Set<Type> scalars;
    private final Set<Type> nonScalars;

    /**
     * Creates a new instance of this class.
     *
     * @param scalarTypes a collection of scalar types
     */
    public BasicTypeHinter(@NotNull Collection<Token<?>> scalarTypes) {
        if (scalarTypes.isEmpty()) {
            this.scalars = Set.of();
            this.nonScalars = Set.of();
        } else {
            this.scalars = Collections.newSetFromMap(new WeakHashMap<>(scalarTypes.size()));
            for (Token<?> token : scalarTypes) {
                this.scalars.add(token.get());
            }

            this.nonScalars = Collections.newSetFromMap(new WeakHashMap<>());
        }
    }

    @Override
    public @NotNull ElementType getHint(@NotNull Token<?> type) {
        if (type.isSubclassOf(ConfigElement.class) || type.isPrimitiveOrWrapper() || type.isSubclassOf(String.class) ||
            type.isEnumType()) {
            //we treat ConfigElement as scalar because it need not be deeply processed, just copied over
            return ElementType.SCALAR;
        } else if (type.isSubclassOf(Map.class) || type.isSubclassOf(Collection.class) || type.isArrayType()) {
            return ElementType.LIST;
        }

        if (scalars.isEmpty()) {
            return ElementType.NODE;
        }

        Type actual = type.get();
        if (scalars.contains(actual)) {
            return ElementType.SCALAR;
        }

        if (!nonScalars.contains(actual)) {
            for (Type existing : scalars) {
                if (type.isSubclassOf(existing)) {
                    return ElementType.SCALAR;
                }
            }

            nonScalars.add(actual);
        }

        return ElementType.NODE;
    }

    @Override
    public boolean assignable(@NotNull ConfigElement element, @NotNull Token<?> toType) {
        if (toType.isSubclassOf(ConfigElement.class)) {
            return toType.isSuperclassOf(element.getClass());
        }

        return switch (element.type()) {
            //if we are a node, we *might* be valid regardless of the target type
            case NODE -> true;
            case LIST -> {
                ElementType hint = getHint(toType);
                if (hint.isList()) {
                    //we know we're a map, collection, or array, so we're compatible
                    yield true;
                }

                //if toType is a superclass of Collection, returns true
                yield toType.isSuperclassOf(Collection.class);
            }
            case SCALAR -> {
                Object scalar = element.asScalar();
                if (scalar == null) {
                    //null is assignable to all types, even non-scalars
                    yield true;
                }

                //if not assignable, check the hint type
                if (!toType.isSuperclassOf(scalar.getClass())) {
                    yield getHint(toType) == ElementType.SCALAR;
                }

                yield true;
            }
        };
    }

    @Override
    public @NotNull Token<?> getPreferredType(@NotNull ConfigElement element, @NotNull Token<?> upperBounds) {
        if (upperBounds.isSubclassOf(ConfigElement.class)) {
            return upperBounds;
        }

        return switch (element.type()) {
            case NODE -> upperBounds; //can't guess type from a node, assume it's assignable
            case LIST -> ARRAY_LIST; //arraylist is preferred, but don't check against the upper bounds (caller should)
            case SCALAR -> {
                Object scalar = element.asScalar();
                if (scalar == null) {
                    //null is assignable to anything
                    yield upperBounds;
                }

                if (!upperBounds.isSuperclassOf(scalar.getClass())) {
                    yield upperBounds;
                }

                yield Token.ofType(scalar.getClass());
            }
        };
    }
}
