package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.core.mapper.Token;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public interface Signature {
    record TypedObject(@Nullable String name, @NotNull Type type, @NotNull Object value) {}

    @NotNull Iterable<Entry<String, Type>> argumentTypes();

    @NotNull Collection<TypedObject> objectData(@NotNull Object object);

    @NotNull ConfigContainer initContainer(int sizeHint);

    default boolean hasBuildingObject() {
        return false;
    }

    default @NotNull Object initBuildingObject(@NotNull ConfigElement element) {
        throw new IllegalStateException("unsupported operation");
    }

    Object buildObject(@Nullable Object buildingObject, Object @NotNull [] args);

    boolean matchesArgumentNames();

    boolean matchesTypeHints();

    int length(@Nullable ConfigElement element);

    @NotNull ElementType typeHint();

    @NotNull Type returnType();

    default int priority() {
        return 0;
    }

    class Builder<T> {
        private final BiFunction<? super T, ? super Object[], ?> constructor;
        private final Type returnType;
        private final Collection<Entry<String, Type>> argumentTypes;
        private final Function<? super T, ? extends Collection<TypedObject>> objectSignatureExtractor;

        private int priority;
        private boolean matchNames;
        private boolean matchTypeHints;
        private ElementType typeHint = ElementType.NODE;
        private Function<? super ConfigElement, ?> buildingObjectInitializer;
        private ToIntFunction<? super ConfigElement> lengthFunction;
        private IntFunction<? extends ConfigContainer> containerFunction = (IntFunction<ConfigContainer>) value -> {
            if (typeHint == ElementType.LIST) {
                return new ArrayConfigList(value);
            } else {
                return new LinkedConfigNode(value);
            }
        };

        public Builder(@NotNull BiFunction<? super T, ? super Object[], ?> constructor,
                @NotNull Token<T> type, @NotNull Collection<Entry<String, Type>> argumentTypes,
                @NotNull Function<? super T, ? extends Collection<TypedObject>> objectSignatureExtractor) {
            this.constructor = Objects.requireNonNull(constructor);
            this.returnType = Objects.requireNonNull(type).get();
            this.argumentTypes = List.copyOf(argumentTypes);
            this.objectSignatureExtractor = Objects.requireNonNull(objectSignatureExtractor);
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

        public @NotNull Builder<T> withBuildingObjectInitializer(@NotNull Function<? super ConfigElement, ?> buildingObjectInitializer) {
            this.buildingObjectInitializer = Objects.requireNonNull(buildingObjectInitializer);
            return this;
        }

        public @NotNull Builder<T> withLengthFunction(@NotNull ToIntFunction<? super ConfigElement> lengthFunction) {
            this.lengthFunction = Objects.requireNonNull(lengthFunction);
            return this;
        }

        public @NotNull Builder<T> withContainerFunction(@NotNull IntFunction<? extends ConfigContainer> containerFunction) {
            this.containerFunction = Objects.requireNonNull(containerFunction);
            return this;
        }

        public @NotNull Signature build() {
            return new Signature() {
                @Override
                public @NotNull Iterable<Entry<String, Type>> argumentTypes() {
                    return argumentTypes;
                }

                @SuppressWarnings("unchecked")
                @Override
                public @NotNull Collection<TypedObject> objectData(@NotNull Object object) {
                    return objectSignatureExtractor.apply((T)object);
                }

                @Override
                public @NotNull ConfigContainer initContainer(int sizeHint) {
                    return containerFunction.apply(sizeHint);
                }

                @SuppressWarnings("unchecked")
                @Override
                public Object buildObject(@Nullable Object buildingObject, Object @NotNull [] args) {
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
                    if (lengthFunction != null) {
                        return lengthFunction.applyAsInt(element);
                    }

                    return argumentTypes.size();
                }

                @Override
                public @NotNull ElementType typeHint() {
                    return typeHint;
                }

                @Override
                public @NotNull Type returnType() {
                    return returnType;
                }

                @Override
                public int priority() {
                    return priority;
                }

                @Override
                public boolean hasBuildingObject() {
                    return buildingObjectInitializer != null;
                }

                @Override
                public @NotNull Object initBuildingObject(@NotNull ConfigElement element) {
                    return buildingObjectInitializer.apply(element);
                }
            };
        }
    }
}
