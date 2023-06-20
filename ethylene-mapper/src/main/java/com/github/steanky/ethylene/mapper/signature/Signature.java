package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.ElementType;
import com.github.steanky.ethylene.core.collection.ArrayConfigList;
import com.github.steanky.ethylene.core.collection.ConfigContainer;
import com.github.steanky.ethylene.core.collection.LinkedConfigNode;
import com.github.steanky.ethylene.mapper.MapperException;
import com.github.steanky.ethylene.mapper.Prioritized;
import com.github.steanky.ethylene.mapper.PrioritizedBase;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents something that may create a particular object from configuration data, and vice-versa. It also supplies
 * information necessary to determine what types are required to construct objects.
 */
public interface Signature<TReturn> extends Prioritized {
    /**
     * Creates a new signature builder.
     *
     * @param type                     the object that will be created by the signature
     * @param constructor              the constructor used by the signature to create objects
     * @param objectSignatureExtractor the function which extracts information about objects of the signature's type
     * @param arguments                the types needed for the signature to create objects
     * @param <T>                      the type of object which will be created by the signature
     * @return a new {@link Signature.Builder} instance
     */
    @SafeVarargs
    static <T> Builder<T> builder(@NotNull Token<T> type,
        @NotNull BiFunction<? super T, ? super Arguments, ? extends T> constructor,
        @NotNull Function<? super T, ? extends Collection<Object>> objectSignatureExtractor,
        @NotNull Map.Entry<String, SignatureParameter> @NotNull ... arguments) {
        return new Builder<>(type, constructor, objectSignatureExtractor, Map.of(), arguments);
    }

    /**
     * Creates a new signature builder.
     *
     * @param type                     the object that will be created by the signature
     * @param constructor              the constructor used by the signature to create objects
     * @param objectSignatureExtractor the function which extracts information about objects of the signature's type
     * @param typeVariableMappings     a mapping of argument names to {@link TypeVariable} tokens, which are used to
     *                                 match generic types during object construction
     * @param arguments                the types needed for the signature to create objects
     * @param <T>                      the type of object which will be created by the signature
     * @return a new {@link Signature.Builder} instance
     */
    @SafeVarargs
    static <T> Builder<T> builder(@NotNull Token<T> type,
        @NotNull BiFunction<? super T, ? super Arguments, ? extends T> constructor,
        @NotNull Function<? super T, ? extends Collection<Object>> objectSignatureExtractor,
        @NotNull Map<String, Token<?>> typeVariableMappings,
        @NotNull Map.Entry<String, SignatureParameter> @NotNull ... arguments) {
        return new Builder<>(type, constructor, objectSignatureExtractor, typeVariableMappings, arguments);
    }

    /**
     * The argument types of this signature. These are the types needed to create this signature's object, in the order
     * that they should be supplied to the signature's constructor. The entry key is the name of the type - this might
     * correspond to a field name or record accessor name. It is pattern-matched against the name of the configuration
     * used to create objects from this signature. In the case of collections or arrays, the name is null.
     * <p>
     * This iterable may be empty, in the case of objects that need no parameters. It may also be "infinite length" and
     * never terminate if iterated in a loop. This is typically the case for container-like objects, which can contain
     * "any number" of elements of the same type. When this occurs, the iterable is expected to return the same
     * {@link Map.Entry} indefinitely. To determine when to stop iterating, call
     * {@link Signature#length(ConfigElement)}.
     * <p>
     * These types represent an "upper bound". For each argument type, it is permissible to instead pass a subtype to
     * the constructor.
     *
     * @return the argument types iterable
     */
    @NotNull @Unmodifiable Iterable<Map.Entry<String, SignatureParameter>> argumentTypes();

    /**
     * Returns the map of {@link TypeVariable}-containing {@link Token}s to argument types.
     *
     * @return the map of this signature's TypeVariables to the equivalent parameter names
     */
    @NotNull @Unmodifiable Map<String, Token<?>> genericMappings();

    /**
     * Extracts some data from a particular object. The returned collection is used to map object data to configuration
     * data.
     *
     * @param object the object from which to extract data
     * @return a collection of {@link TypedObject} representing this object's data
     */
    @NotNull @Unmodifiable Collection<TypedObject> objectData(@NotNull TReturn object);

    /**
     * Initializes the {@link ConfigContainer} which will later be populated with object data. Calling this method
     * should not, on its own, add elements to the returned container.
     *
     * @param sizeHint the size hint
     * @return the ConfigContainer which will later be populated with elements
     */
    @NotNull ConfigContainer initContainer(int sizeHint);

    /**
     * Determines if this signature supports "building objects". Building objects are objects that can be initialized
     * <i>prior</i> to being populated with data. This allows them to support circular references.
     *
     * @return true if this signature supports building objects; false otherwise
     */
    default boolean hasBuildingObject() {
        return false;
    }

    /**
     * Initializes the building object for this signature. If building objects are not supported (i.e.
     * {@link Signature#hasBuildingObject()} returns false), an {@link UnsupportedOperationException} is thrown.
     *
     * @param element the element from which to initialize the building object
     * @return the initialized building object which will later be populated with data
     */
    default @NotNull TReturn initBuildingObject(@NotNull ConfigElement element) {
        throw new UnsupportedOperationException("This signature does not support pre-initialized building objects");
    }

    /**
     * Builds an object.
     *
     * @param buildingObject the building object; may be null if {@link Signature#hasBuildingObject()} returns false
     * @param args           the arguments from which to create or populate an object
     * @return a new object if this signature does not have a building object; otherwise, if a building object is
     * provided, populates it
     */
    @NotNull TReturn buildObject(@Nullable TReturn buildingObject, Object @NotNull [] args);

    /**
     * Whether this signature should respect argument names when being matched.
     *
     * @return true if this signature respects argument names, false otherwise
     */
    boolean matchesArgumentNames();

    /**
     * Whether this signature should respect type hints when being matched.
     *
     * @return true if this signature should respect type hints, false otherwise
     */
    boolean matchesTypeHints();

    /**
     * Determines the number of arguments this signature wants, given a {@link ConfigElement}. May return -1 to indicate
     * that no length can be determined given the provided element.
     *
     * @param element the element from which to determine a length; will be null if no element is known (as is the case
     *                when mapping object data to a ConfigElement)
     * @return the length of this signature
     */
    int length(@Nullable ConfigElement element);

    /**
     * Gets the return type of this signature. This is the type of the object it produces.
     *
     * @return the type of the object this signature produces
     */
    @NotNull Token<TReturn> returnType();

    /**
     * Arguments used to construct an object.
     */
    class Arguments {
        private final Object[] arguments;

        private Arguments(Object[] arguments) {
            this.arguments = arguments;
        }

        /**
         * Gets the object at the given index, after casting to the specific type.
         *
         * @param index the index at which to locate an argument
         * @param <T>   the type of the object
         * @return the object at the given index
         */
        @SuppressWarnings("unchecked")
        public <T> T get(int index) {
            return (T) arguments[index];
        }

        /**
         * The number of arguments present.
         *
         * @return the number of arguments
         */
        public int length() {
            return arguments.length;
        }
    }

    /**
     * An object which has generic type information and optionally a name and/or default value associated with it.
     *
     * @param name  the name of this typed object
     * @param type  the generic type information associated with the object
     * @param value the object itself
     * @param defaultValue the default value for this object
     */
    record TypedObject(@Nullable String name, @NotNull Token<?> type, @Nullable Object value,
                       @Nullable ConfigElement defaultValue) {
        /**
         * Creates a new instance of this record.
         *
         * @param name  the name of this TypedObject; can be null to indicate no name
         * @param type  the type associated with the value object
         * @param value the value object itself
         */
        public TypedObject(@Nullable String name, @NotNull Token<?> type, @Nullable Object value,
            @Nullable ConfigElement defaultValue) {
            this.name = name;
            this.type = Objects.requireNonNull(type);
            this.value = value;
            this.defaultValue = defaultValue;
        }

        /**
         * Creates a new instance of this record with a null default value.
         *
         * @param name  the name of this TypedObject; can be null to indicate no name
         * @param type  the type associated with the value object
         * @param value the value object itself
         */
        public TypedObject(@Nullable String name, @NotNull Token<?> type, Object value) {
            this(name, type, value, null);
        }

        /**
         * Creates a new instance of this record with a null name and default value.
         *
         * @param type  the type associated with the value object
         * @param value the value object itself
         */
        public TypedObject(@NotNull Token<?> type, Object value) {
            this(null, type, value, null);
        }
    }

    /**
     * Signature implementation. Used internally by {@link Signature.Builder}. Not part of the public API.
     */
    @ApiStatus.Internal
    final class SignatureImpl<T> extends PrioritizedBase implements Signature<T> {
        private final Collection<Map.Entry<String, SignatureParameter>> argumentTypes;
        private final Map<String, Token<?>> typeVariableMappings;
        private final Function<? super T, ? extends Collection<Object>> objectSignatureExtractor;
        private final BiFunction<? super ElementType, ? super Integer, ? extends ConfigContainer> containerFunction;
        private final Function<? super ConfigElement, ? extends T> buildingObjectInitializer;
        private final BiFunction<? super T, ? super Arguments, ? extends T> constructor;
        private final boolean matchNames;
        private final boolean matchTypeHints;
        private final BiFunction<? super Collection<? extends Map.Entry<String, SignatureParameter>>, ? super ConfigElement, ?
            extends Integer>
            lengthFunction;
        private final ElementType typeHint;
        private final Token<T> returnType;

        private SignatureImpl(int priority, Collection<Map.Entry<String, SignatureParameter>> argumentTypes,
            Map<String, Token<?>> typeVariableMappings,
            Function<? super T, ? extends Collection<Object>> objectSignatureExtractor,
            BiFunction<? super ElementType, ? super Integer, ? extends ConfigContainer> containerFunction,
            Function<? super ConfigElement, ? extends T> buildingObjectInitializer,
            BiFunction<? super T, ? super Arguments, ? extends T> constructor, boolean matchNames,
            boolean matchTypeHints,
            BiFunction<? super Collection<? extends Map.Entry<String, SignatureParameter>>, ? super ConfigElement, ?
                extends Integer> lengthFunction,
            ElementType typeHint, Token<T> returnType) {
            super(priority);
            this.argumentTypes = argumentTypes;
            this.typeVariableMappings = typeVariableMappings;
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
        public @NotNull Iterable<Map.Entry<String, SignatureParameter>> argumentTypes() {
            return argumentTypes;
        }

        @Override
        public @NotNull @Unmodifiable Map<String, Token<?>> genericMappings() {
            return typeVariableMappings;
        }

        @Override
        public @NotNull Collection<TypedObject> objectData(@NotNull T object) {
            Collection<Object> args = objectSignatureExtractor.apply(object);
            if (args.size() != argumentTypes.size()) {
                throw new MapperException(
                    "Unexpected number of arguments; needed " + argumentTypes.size() + ", got " + args.size());
            }

            return new AbstractCollection<>() {
                @Override
                public Iterator<TypedObject> iterator() {
                    return new Iterator<>() {
                        private final Iterator<Object> objectIterator = args.iterator();
                        private final Iterator<Map.Entry<String, SignatureParameter>> entryIterator = argumentTypes.iterator();

                        @Override
                        public boolean hasNext() {
                            return objectIterator.hasNext() && entryIterator.hasNext();
                        }

                        @Override
                        public TypedObject next() {
                            Object object = objectIterator.next();
                            Map.Entry<String, SignatureParameter> entry = entryIterator.next();
                            return new TypedObject(entry.getKey(), entry.getValue().type(), object,
                                entry.getValue().defaultOption());
                        }
                    };
                }

                @Override
                public int size() {
                    return argumentTypes.size();
                }
            };
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
        public @NotNull T initBuildingObject(@NotNull ConfigElement element) {
            if (buildingObjectInitializer == null) {
                //throws an exception
                return Signature.super.initBuildingObject(element);
            }

            return buildingObjectInitializer.apply(element);
        }

        @Override
        public @NotNull T buildObject(@Nullable T buildingObject, Object @NotNull [] args) {
            return constructor.apply(buildingObject, new Arguments(args));
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
        public @NotNull Token<T> returnType() {
            return returnType;
        }
    }

    /**
     * Utility builder which can be used to create custom {@link Signature} implementations.
     *
     * @param <T> the type of object this signature produces
     */
    final class Builder<T> {
        private final BiFunction<? super T, ? super Arguments, ? extends T> constructor;
        private final Token<T> returnType;
        private final Function<? super T, ? extends Collection<Object>> objectSignatureExtractor;
        private final Collection<Map.Entry<String, SignatureParameter>> argumentTypes;
        private final Map<String, Token<?>> typeVariableMappings;

        private int priority;
        private boolean matchNames;
        private boolean matchTypeHints;
        private ElementType typeHint = ElementType.NODE;
        private Function<? super ConfigElement, ? extends T> buildingObjectInitializer;
        private BiFunction<? super Collection<? extends Map.Entry<String, SignatureParameter>>, ? super ConfigElement, ?
            extends Integer>
            lengthFunction = (types, element) -> types.size();

        private BiFunction<? super ElementType, ? super Integer, ? extends ConfigContainer> containerFunction =
            (elementType, size) -> {
                if (elementType == ElementType.LIST) {
                    return new ArrayConfigList(size);
                } else {
                    return new LinkedConfigNode(size);
                }
            };

        @SafeVarargs
        private Builder(@NotNull Token<T> returnType,
            @NotNull BiFunction<? super T, ? super Arguments, ? extends T> constructor,
            @NotNull Function<? super T, ? extends Collection<Object>> objectSignatureExtractor,
            @NotNull Map<String, Token<?>> typeVariableMappings,
            @NotNull Map.Entry<String, SignatureParameter> @NotNull ... arguments) {
            this.constructor = Objects.requireNonNull(constructor);
            this.returnType = Objects.requireNonNull(returnType);
            this.objectSignatureExtractor = Objects.requireNonNull(objectSignatureExtractor);

            this.typeVariableMappings = Map.copyOf(typeVariableMappings);

            for (Token<?> token : typeVariableMappings.values()) {
                if (!(token.get() instanceof TypeVariable<?>)) {
                    throw new IllegalArgumentException(
                        "TypeVariable mapping tokens may only contain TypeVariable instances");
                }
            }

            this.argumentTypes = new ArrayList<>(arguments.length);
            for (Map.Entry<String, SignatureParameter> entry : arguments) {
                argumentTypes.add(Map.entry(entry.getKey(), entry.getValue()));
            }
        }

        /**
         * Specifies a custom priority for the signature.
         *
         * @param priority the new priority
         * @return this builder, for chaining
         */
        public @NotNull Builder<T> withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Specifies that the signature should match argument names.
         *
         * @return this builder, for chaining
         */
        public @NotNull Builder<T> matchingNames() {
            this.matchNames = true;
            return this;
        }

        /**
         * Specifies that this signature should match type hints.
         *
         * @return this builder, for chaining
         */
        public @NotNull Builder<T> matchingTypeHints() {
            this.matchTypeHints = true;
            return this;
        }

        /**
         * Specifies the type hint of this signature.
         *
         * @param typeHint the type hint of this signature
         * @return this builder, for chaining
         */
        public @NotNull Builder<T> withTypeHint(@NotNull ElementType typeHint) {
            this.typeHint = Objects.requireNonNull(typeHint);
            return this;
        }

        /**
         * Supplies a building object initializer for this signature. Doing so implies that the signature supports
         * building objects, which will enable resolution of circular references.
         *
         * @param buildingObjectInitializer the object initializer
         * @return this builder, for chaining
         */
        public @NotNull Builder<T> withBuildingObjectInitializer(
            @NotNull Function<? super ConfigElement, ? extends T> buildingObjectInitializer) {
            this.buildingObjectInitializer = Objects.requireNonNull(buildingObjectInitializer);
            return this;
        }

        /**
         * Specifies a function used to compute the signature's length, given a {@link ConfigElement}.
         *
         * @param lengthFunction the length function
         * @return this builder, for chaining
         */
        public @NotNull Builder<T> withLengthFunction(
            @NotNull BiFunction<? super Collection<? extends Map.Entry<String, SignatureParameter>>, ? super ConfigElement, ?
                extends Integer> lengthFunction) {
            this.lengthFunction = Objects.requireNonNull(lengthFunction);
            return this;
        }

        /**
         * Specifies a function used to produce {@link ConfigContainer}s, for use when converting object data to
         * configuration data.
         *
         * @param containerFunction the function which creates a new {@link ConfigContainer} implementation from the
         *                          provided type hint and length
         * @return this builder, for chaining
         */
        public @NotNull Builder<T> withContainerFunction(
            @NotNull BiFunction<? super ElementType, ? super Integer, ? extends ConfigContainer> containerFunction) {
            this.containerFunction = Objects.requireNonNull(containerFunction);
            return this;
        }

        /**
         * Builds the signature according to the parameters outlined by the builder.
         *
         * @return a new {@link Signature} implementation
         */
        public @NotNull Signature<T> build() {
            return new SignatureImpl<>(priority, List.copyOf(argumentTypes), typeVariableMappings,
                objectSignatureExtractor, containerFunction, buildingObjectInitializer, constructor, matchNames,
                matchTypeHints, lengthFunction, typeHint, returnType);
        }
    }
}
