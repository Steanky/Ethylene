package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.mapper.Prioritized;
import com.github.steanky.ethylene.mapper.PrioritizedBase;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/**
 * Represents something that may create a particular object from configuration data, and vice-versa. It also supplies
 * information necessary to determine what types are required to construct objects.
 */
public interface Signature<TReturn> extends Prioritized {
    @SafeVarargs
    static <T> Builder<T> builder(@NotNull Token<T> type,
        @NotNull BiFunction<? super T, ? super Object[], ?> constructor,
        @NotNull Function<? super T, ? extends Collection<TypedObject>> objectSignatureExtractor,
        @NotNull Map.Entry<String, Token<?>> @NotNull ... arguments) {
        return new Builder<>(type, constructor, objectSignatureExtractor, arguments);
    }

    static @NotNull TypedObject type(@NotNull String name, @NotNull Token<?> type, @NotNull Object value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        Objects.requireNonNull(value);
        return new TypedObject(name, type, value);
    }

    static @NotNull TypedObject type(@NotNull Token<?> type, @NotNull Object value) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(value);
        return new TypedObject(null, type, value);
    }

    @NotNull Iterable<Map.Entry<String, Token<?>>> argumentTypes();

    @NotNull Collection<TypedObject> objectData(@NotNull Object object);

    @NotNull ConfigContainer initContainer(int sizeHint);

    default boolean hasBuildingObject() {
        return false;
    }

    default @NotNull Object initBuildingObject(@NotNull ConfigElement element) {
        throw new UnsupportedOperationException("This signature does not support pre-initialized building objects");
    }

    @NotNull Object buildObject(@Nullable Object buildingObject, Object @NotNull [] args);

    boolean matchesArgumentNames();

    boolean matchesTypeHints();

    int length(@Nullable ConfigElement element);

    @NotNull ElementType typeHint();

    @NotNull Token<TReturn> returnType();

    record TypedObject(@Nullable String name, @NotNull Token<?> type, @NotNull Object value) {
    }

    /**
     * Signature implementation. Used internally by {@link Signature.Builder}. Not part of the public API.
     */
    @ApiStatus.Internal
    class SignatureImpl<T> extends PrioritizedBase implements Signature<T> {
        private final Collection<Map.Entry<String, Token<?>>> argumentTypes;
        private final Function<? super T, ? extends Collection<TypedObject>> objectSignatureExtractor;
        private final BiFunction<? super ElementType, Integer, ? extends ConfigContainer> containerFunction;
        private final Function<? super ConfigElement, ?> buildingObjectInitializer;
        private final BiFunction<? super T, ? super Object[], ?> constructor;
        private final boolean matchNames;
        private final boolean matchTypeHints;
        private final BiFunction<? super Collection<? extends Map.Entry<String, Token<?>>>, ? super ConfigElement, Integer>
            lengthFunction;
        private final ElementType typeHint;
        private final Token<T> returnType;

        private SignatureImpl(int priority,
            Collection<Map.Entry<String, Token<?>>> argumentTypes,
            Function<? super T, ? extends Collection<TypedObject>> objectSignatureExtractor,
            BiFunction<? super ElementType, Integer, ? extends ConfigContainer> containerFunction,
            Function<? super ConfigElement, ?> buildingObjectInitializer,
            BiFunction<? super T, ? super Object[], ?> constructor, boolean matchNames, boolean matchTypeHints,
            BiFunction<? super Collection<? extends Map.Entry<String, Token<?>>>, ? super ConfigElement, Integer>
                lengthFunction, ElementType typeHint, Token<T> returnType) {
            super(priority);
            this.argumentTypes = argumentTypes;
            this.objectSignatureExtractor = objectSignatureExtractor;
            this.containerFunction = containerFunction;
            this.buildingObjectInitializer = buildingObjectInitializer;
            this.constructor = constructor;
            this.matchNames = matchNames;
            this.matchTypeHints = matchTypeHints;
            this.lengthFunction = lengthFunction;
            this.typeHint = typeHint;
            this.returnType = returnType;
        }

        @Override
        public @NotNull Iterable<Map.Entry<String, Token<?>>> argumentTypes() {
            return argumentTypes;
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull Collection<TypedObject> objectData(@NotNull Object object) {
            return objectSignatureExtractor.apply((T) object);
        }

        @Override
        public @NotNull ConfigContainer initContainer(int sizeHint) {
            return containerFunction.apply(typeHint, sizeHint);
        }

        @Override
        public boolean hasBuildingObject() {
            return buildingObjectInitializer != null;
        }

        @Override
        public @NotNull Object initBuildingObject(@NotNull ConfigElement element) {
            if (buildingObjectInitializer == null) {
                //throws an exception
                return Signature.super.initBuildingObject(element);
            }

            return buildingObjectInitializer.apply(element);
        }

        @SuppressWarnings("unchecked")
        @Override
        public @NotNull Object buildObject(@Nullable Object buildingObject, Object @NotNull [] args) {
            return constructor.apply((T) buildingObject, args);
        }

        @Override
        public boolean matchesArgumentNames() {
            return matchNames;
        }

        @Override
        public boolean matchesTypeHints() {
            return matchTypeHints;
        }

        @Override
        public int length(@Nullable ConfigElement element) {
            return lengthFunction.apply(argumentTypes, element);
        }

        @Override
        public @NotNull ElementType typeHint() {
            return typeHint;
        }

        @Override
        public @NotNull Token<T> returnType() {
            return returnType;
        }
    }

    /**
     * Utility builder which can be used to create custom {@link Signature} implementations.
     *
     * @param <T> the type of object this signature produces
     */
    class Builder<T> {
        private final BiFunction<? super T, ? super Object[], ?> constructor;
        private final Token<T> returnType;
        private final Function<? super T, ? extends Collection<TypedObject>> objectSignatureExtractor;
        private final Collection<Map.Entry<String, Token<?>>> argumentTypes;

        private int priority;
        private boolean matchNames;
        private boolean matchTypeHints;
        private ElementType typeHint = ElementType.NODE;
        private Function<? super ConfigElement, ?> buildingObjectInitializer;
        private BiFunction<? super Collection<? extends Map.Entry<String, Token<?>>>, ? super ConfigElement, Integer>
            lengthFunction = (types, element) -> types.size();

        private BiFunction<? super ElementType, Integer, ? extends ConfigContainer> containerFunction =
            (elementType, size) -> {
            if (elementType == ElementType.LIST) {
                return new ArrayConfigList(size);
            } else {
                return new LinkedConfigNode(size);
            }
        };

        @SafeVarargs
        private Builder(@NotNull Token<T> returnType, @NotNull BiFunction<? super T, ? super Object[], ?> constructor,
            @NotNull Function<? super T, ? extends Collection<TypedObject>> objectSignatureExtractor,
            @NotNull Map.Entry<String, Token<?>> @NotNull ... arguments) {
            this.constructor = Objects.requireNonNull(constructor);
            this.returnType = Objects.requireNonNull(returnType);
            this.objectSignatureExtractor = Objects.requireNonNull(objectSignatureExtractor);

            this.argumentTypes = new ArrayList<>(arguments.length);
            for (Map.Entry<String, Token<?>> entry : arguments) {
                argumentTypes.add(Map.entry(entry.getKey(), entry.getValue()));
            }
        }

        public @NotNull Builder<T> withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public @NotNull Builder<T> matchingNames() {
            this.matchNames = true;
            return this;
        }

        public @NotNull Builder<T> matchingTypeHints() {
            this.matchTypeHints = true;
            return this;
        }

        public @NotNull Builder<T> withTypeHint(@NotNull ElementType typeHint) {
            this.typeHint = Objects.requireNonNull(typeHint);
            return this;
        }

        public @NotNull Builder<T> withBuildingObjectInitializer(
            @NotNull Function<? super ConfigElement, ?> buildingObjectInitializer) {
            this.buildingObjectInitializer = Objects.requireNonNull(buildingObjectInitializer);
            return this;
        }

        public @NotNull Builder<T> withLengthFunction(@NotNull BiFunction<? super Collection<? extends
            Map.Entry<String, Token<?>>>, ? super ConfigElement, Integer> lengthFunction) {
            this.lengthFunction = Objects.requireNonNull(lengthFunction);
            return this;
        }

        public @NotNull Builder<T> withContainerFunction(
            @NotNull BiFunction<? super ElementType, Integer, ? extends ConfigContainer> containerFunction) {
            this.containerFunction = Objects.requireNonNull(containerFunction);
            return this;
        }

        public @NotNull Signature<T> build() {
            return new SignatureImpl<>(priority, List.copyOf(argumentTypes), objectSignatureExtractor,
                containerFunction, buildingObjectInitializer, constructor, matchNames, matchTypeHints, lengthFunction,
                typeHint, returnType);
        }
    }
}
