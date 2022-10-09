package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.github.steanky.ethylene.mapper.internal.CollectionUtils;
import com.github.steanky.ethylene.mapper.signature.*;
import com.github.steanky.ethylene.mapper.signature.field.FieldSignatureBuilder;
import com.github.steanky.ethylene.mapper.type.Token;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A source of {@link MappingConfigProcessor} instances. Also contains a builder for convenience.
 */
public interface MappingProcessorSource {
    /**
     * Creates a new {@link MappingProcessorSource.Builder}.
     *
     * @return a new MappingProcessorSource.Builder
     */
    static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Supplies a {@link ConfigProcessor} capable of processing the given data type.
     *
     * @param token   the type of data to process
     * @param <TData> the type of data which can be processed
     * @return a ConfigProcessor which may process the given data type
     */
    <TData> @NotNull ConfigProcessor<TData> processorFor(@NotNull Token<TData> token);

    /**
     * The built-in builder used to create {@link MappingProcessorSource} instances. Includes methods to allow the
     * creation of custom implementations of various components.
     * <p>
     * All methods, unless otherwise noted, will throw a {@link NullPointerException} if null is passed as an argument,
     * or an entry/collection containing null is given.
     */
    class Builder {
        private static final Signature MAP_ENTRY_SIGNATURE =
            Signature.builder(Token.ofClass(Map.Entry.class), (entry, objects) -> Entry.of(objects[0], objects[1]),
                (entry) -> List.of(Signature.type("key", Token.OBJECT, entry.getKey()),
                    Signature.type("value", Token.OBJECT, entry.getValue())), Entry.of("key", Token.OBJECT),
                Entry.of("value", Token.OBJECT)).matchingTypeHints().matchingNames().build();

        private final Collection<Signature> customSignatures = new HashSet<>();
        private final Collection<Map.Entry<Class<?>, Class<?>>> typeImplementations = new HashSet<>();
        private final Collection<Map.Entry<Class<?>, ? extends SignatureBuilder>> signatureBuilderPreferences =
            new HashSet<>();
        private final Collection<Token<?>> scalarTypes = new HashSet<>();
        private final Collection<ScalarSignature<?>> scalarSignatures = new HashSet<>();

        private SignatureBuilder defaultSignatureBuilder = FieldSignatureBuilder.INSTANCE;

        private Function<? super Collection<Token<?>>, ? extends TypeHinter> typeHinterFunction = BasicTypeHinter::new;
        private BiFunction<? super TypeHinter, ? super Collection<? extends ScalarSignature<?>>, ? extends ScalarSource>
            scalarSourceFunction = BasicScalarSource::new;
        private BiFunction<? super SignatureBuilder, ? super Collection<? extends Map.Entry<Class<?>, ?
            extends SignatureBuilder>>, ? extends SignatureBuilder.Selector>
            signatureBuilderSelectorFunction = BasicSignatureBuilderSelector::new;
        private TriFunction<? super TypeHinter, ? super SignatureBuilder.Selector, ? super Collection<Signature>, ?
            extends SignatureMatcher.Source>
            signatureMatcherSourceFunction = BasicSignatureMatcherSource::new;
        private BiFunction<? super TypeHinter, ? super Collection<? extends Map.Entry<Class<?>, Class<?>>>, ?
            extends TypeResolver>
            typeResolverFunction = BasicTypeResolver::new;

        Builder() {
        }

        /**
         * Supplies a {@link Function} which may be used to construct a custom {@link TypeHinter}.
         *
         * @param typeHinterFunction the function used to create TypeHinters, which accepts a collection of
         *                           {@link Token} objects corresponding to custom scalar types
         * @return this builder, for chaining
         */
        public @NotNull Builder withTypeHinterFunction(
            @NotNull Function<? super Collection<Token<?>>, ? extends TypeHinter> typeHinterFunction) {
            this.typeHinterFunction = Objects.requireNonNull(typeHinterFunction);
            return this;
        }

        /**
         * Adds standard signature(s) to this builder. Currently, this only includes {@link Map.Entry}.
         *
         * @return this builder, for chaining
         */
        public @NotNull Builder withStandardSignatures() {
            customSignatures.add(MAP_ENTRY_SIGNATURE);
            return this;
        }

        /**
         * Adds standard type implementations to this builders. This will allow the resulting {@link ConfigProcessor}s
         * to process {@link Collection}, {@link Map}, and {@link Set} types.
         *
         * @return this builder, for chaining
         */
        public @NotNull Builder withStandardTypeImplementations() {
            typeImplementations.add(Map.entry(ArrayList.class, Collection.class));
            typeImplementations.add(Map.entry(HashMap.class, Map.class));
            typeImplementations.add(Map.entry(HashSet.class, Set.class));
            return this;
        }

        /**
         * Supplies a {@link BiFunction} which can be used to construct custom implementations of
         * {@link SignatureBuilder.Selector}. The function accepts a {@link SignatureBuilder} to be used as default, and
         * a collection of map entries linking specific types to their preferred {@link SignatureBuilder}
         * implementations.
         *
         * @param function the selector function
         * @return this builder, for chaining
         */
        public @NotNull Builder withSignatureBuilderSelectorFunction(
            @NotNull BiFunction<? super SignatureBuilder, ? super Collection<? extends Map.Entry<Class<?>, ?
                extends SignatureBuilder>>, ? extends SignatureBuilder.Selector> function) {
            this.signatureBuilderSelectorFunction = Objects.requireNonNull(function);
            return this;
        }

        /**
         * Supplies a {@link BiFunction} which can be used to construct custom implementations of {@link TypeResolver},
         * given a {@link TypeHinter} and a collection of implementation-supertype entries.
         *
         * @param function the function used to create the custom TypeResolver
         * @return this builder, for chaining
         */
        public @NotNull Builder withTypeResolverFunction(
            @NotNull BiFunction<? super TypeHinter, ? super Collection<? extends Map.Entry<Class<?>, Class<?>>>, ?
                extends TypeResolver> function) {
            this.typeResolverFunction = Objects.requireNonNull(function);
            return this;
        }

        /**
         * Supplies a {@link BiFunction} which can be used to construct custom implementations of {@link ScalarSource},
         * given a {@link TypeHinter} and collection of {@link ScalarSignature}s.
         *
         * @param function the function used to create the ScalarSource
         * @return this builder, for chaining
         */
        public @NotNull Builder withScalarSourceFunction(
            @NotNull BiFunction<? super TypeHinter, ? super Collection<? extends ScalarSignature<?>>, ?
                extends ScalarSource> function) {
            this.scalarSourceFunction = Objects.requireNonNull(function);
            return this;
        }

        /**
         * Specifies a default {@link SignatureBuilder}.
         *
         * @param defaultBuilder the default builder to be used
         * @return this builder, for chaining
         */
        public @NotNull Builder withDefaultBuilder(@NotNull SignatureBuilder defaultBuilder) {
            this.defaultSignatureBuilder = Objects.requireNonNull(defaultBuilder);
            return this;
        }

        /**
         * Specifies a {@link TriFunction} which may be used to construct custom implementations of
         * {@link SignatureMatcher.Source}, given a collection of {@link Signature} objects corresponding to all custom
         * signatures.
         *
         * @param function the function used to construct SignatureMatcher.Source objects
         * @return this builder, for chaining
         */
        public @NotNull Builder withSignatureMatcherSourceFunction(
            @NotNull TriFunction<? super TypeHinter, ? super SignatureBuilder.Selector, ? super Collection<?
                extends Signature>, ? extends SignatureMatcher.Source> function) {
            this.signatureMatcherSourceFunction = Objects.requireNonNull(function);
            return this;
        }

        /**
         * Specifies a single custom signature.
         *
         * @param signature the custom signature to add
         * @return this builder, for chaining
         */
        public @NotNull Builder withCustomSignature(@NotNull Signature signature) {
            customSignatures.add(Objects.requireNonNull(signature));
            return this;
        }

        /**
         * Specifies a number of custom signatures.
         *
         * @param signatures a collection of custom signatures to add
         * @return this builder, for chaining
         */
        public @NotNull Builder withCustomSignatures(@NotNull Iterable<? extends Signature> signatures) {
            CollectionUtils.addAll(signatures, customSignatures);
            return this;
        }

        /**
         * Specifies a custom scalar signature.
         *
         * @param signature the custom scalar signature
         * @return this builder, for chaining
         */
        public @NotNull Builder withScalarSignature(@NotNull ScalarSignature<?> signature) {
            scalarSignatures.add(Objects.requireNonNull(signature));
            return this;
        }

        /**
         * Specifies a number of custom scalar signatures.
         *
         * @param signatures the scalar signatures to add
         * @return this builder, for chaining
         */
        public @NotNull Builder withScalarSignatures(@NotNull Iterable<? extends ScalarSignature<?>> signatures) {
            CollectionUtils.addAll(signatures, scalarSignatures);
            return this;
        }

        /**
         * Specifies an implementation-superclass pair.
         *
         * @param implementation the implementation
         * @param superclass     the superclass
         * @return this builder, for chaining
         */

        public @NotNull Builder withTypeImplementation(@NotNull Class<?> implementation, @NotNull Class<?> superclass) {
            typeImplementations.add(Map.entry(implementation, superclass));
            return this;
        }

        /**
         * Specifies a number of implementation-superclass pairs.
         *
         * @param entries the implementation-superclass pairs
         * @return this builder, for chaining
         */
        public @NotNull Builder withTypeImplementations(
            @NotNull Iterable<? extends Map.Entry<Class<?>, Class<?>>> entries) {
            CollectionUtils.addAll(entries, typeImplementations);
            return this;
        }

        /**
         * Specifies a {@link SignatureBuilder} preference for the given class.
         *
         * @param type             the class
         * @param signatureBuilder the builder preference
         * @return this builder, for chaining
         */
        public @NotNull Builder withSignatureBuilderPreference(@NotNull Class<?> type,
            @NotNull SignatureBuilder signatureBuilder) {
            signatureBuilderPreferences.add(Map.entry(type, signatureBuilder));
            return this;
        }

        /**
         * Specifies a number of {@link SignatureBuilder} preferences.
         *
         * @param entries the type-signature builder pairs to register
         * @return this builder, for chaining
         */
        public @NotNull Builder withSignatureBuilderPreferences(
            @NotNull Iterable<? extends Map.Entry<Class<?>, ? extends SignatureBuilder>> entries) {
            CollectionUtils.addAll(entries, signatureBuilderPreferences);
            return this;
        }

        /**
         * Builds a new {@link MappingProcessorSource} given the builder's current parameters. This method can be called
         * multiple times in order to create multiple unique instances. If the builder's parameters change between
         * invocations of this method, MappingProcessorSources created prior to the changes will <i>not</i> reflect
         * these changes; but new ones will.
         *
         * @return a new MappingProcessorSource
         */
        public @NotNull MappingProcessorSource build() {
            TypeHinter hinter = typeHinterFunction.apply(scalarTypes);
            SignatureBuilder.Selector selector =
                signatureBuilderSelectorFunction.apply(defaultSignatureBuilder, signatureBuilderPreferences);
            SignatureMatcher.Source source = signatureMatcherSourceFunction.apply(hinter, selector, customSignatures);
            TypeResolver resolver = typeResolverFunction.apply(hinter, typeImplementations);
            ScalarSource scalarSource = scalarSourceFunction.apply(hinter, scalarSignatures);

            return new MappingProcessorSource() {
                @Override
                public @NotNull <T> ConfigProcessor<T> processorFor(@NotNull Token<T> token) {
                    return new MappingConfigProcessor<>(token, source, hinter, resolver, scalarSource);
                }
            };
        }
    }
}
