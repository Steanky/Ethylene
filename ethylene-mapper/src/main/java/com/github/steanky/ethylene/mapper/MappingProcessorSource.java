package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.github.steanky.ethylene.mapper.signature.BasicSignatureBuilderSelector;
import com.github.steanky.ethylene.mapper.signature.Signature;
import com.github.steanky.ethylene.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.mapper.signature.SignatureMatcher;
import com.github.steanky.ethylene.mapper.signature.field.FieldSignatureBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public interface MappingProcessorSource {
    static @NotNull Builder builder() {
        return new Builder();
    }

    <TData> @NotNull ConfigProcessor<TData> processorFor(@NotNull Token<TData> token);

    class Builder {
        @SuppressWarnings("rawtypes")
        private static final Signature MAP_ENTRY_SIGNATURE = Signature.builder(new Token<Map.Entry>() {},
                (entry, objects) -> Map.entry(objects[0], objects[1]),
                (entry) -> List.of(Signature.type("key", Token.OBJECT, entry.getKey()),
                        Signature.type("value", Token.OBJECT, entry.getValue())), Entry.of("key", Token.OBJECT),
                Entry.of("value", Token.OBJECT)).matchingTypeHints().matchingNames().build();

        private final Collection<Signature> customSignatures = new HashSet<>();
        private final Collection<Entry<Class<?>, Class<?>>> typeImplementations = new HashSet<>();
        private final Collection<Entry<Class<?>, SignatureBuilder>> signatureBuilderPreferences = new HashSet<>();

        private TypeHinter typeHinter = BasicTypeHinter.INSTANCE;
        private SignatureBuilder defaultBuilder = FieldSignatureBuilder.INSTANCE;
        private ScalarSource scalarSource = BasicScalarSource.INSTANCE;

        private Function<? super Collection<Signature>, ? extends SignatureMatcher.Source>
                signatureMatcherSourceFunction = customSignatures -> new BasicSignatureMatcherSource(typeHinter,
                getSignatureBuilderSelector(), customSignatures);
        private Function<? super Collection<Entry<Class<?>, SignatureBuilder>>, ? extends SignatureBuilder.Selector>
                signatureBuilderSelectorFunction = signaturePreferences ->
                new BasicSignatureBuilderSelector(defaultBuilder, signaturePreferences);
        private Function<? super Collection<Entry<Class<?>, Class<?>>>, ? extends TypeResolver> typeResolverFunction =
                typeImplementations -> new BasicTypeResolver(typeHinter, typeImplementations);

        Builder() {}

        private SignatureBuilder.Selector getSignatureBuilderSelector() {
            return signatureBuilderSelectorFunction.apply(signatureBuilderPreferences);
        }

        public @NotNull Builder withStandardSignatures() {
            customSignatures.add(MAP_ENTRY_SIGNATURE);
            return this;
        }

        public @NotNull Builder withStandardTypeImplementations() {
            typeImplementations.add(Entry.of(ArrayList.class, Collection.class));
            typeImplementations.add(Entry.of(HashMap.class, Map.class));
            typeImplementations.add(Entry.of(HashSet.class, Set.class));
            return this;
        }

        public @NotNull Builder withSignatureBuilderSelectorFunction(
                @NotNull Function<? super Collection<Entry<Class<?>, SignatureBuilder>>, ? extends SignatureBuilder.Selector> function) {
            this.signatureBuilderSelectorFunction = Objects.requireNonNull(function);
            return this;
        }

        public @NotNull Builder withTypeResolverFunction(
                @NotNull Function<? super Collection<Entry<Class<?>, Class<?>>>, ? extends TypeResolver> function) {
            this.typeResolverFunction = Objects.requireNonNull(function);
            return this;
        }

        public @NotNull Builder withScalarSource(@NotNull ScalarSource scalarSource) {
            this.scalarSource = Objects.requireNonNull(scalarSource);
            return this;
        }

        public @NotNull Builder withDefaultBuilder(@NotNull SignatureBuilder defaultBuilder) {
            this.defaultBuilder = Objects.requireNonNull(defaultBuilder);
            return this;
        }

        public @NotNull Builder withSignatureMatcherSourceFunction(@NotNull Function<? super Collection<Signature>,
                ? extends SignatureMatcher.Source> function) {
            this.signatureMatcherSourceFunction = Objects.requireNonNull(function);
            return this;
        }

        public @NotNull Builder withTypeHinter(@NotNull TypeHinter typeHinter) {
            Objects.requireNonNull(typeHinter);
            this.typeHinter = Objects.requireNonNull(typeHinter);
            return this;
        }

        public @NotNull Builder withCustomSignature(@NotNull Signature signature) {
            customSignatures.add(Objects.requireNonNull(signature));
            return this;
        }

        public @NotNull Builder withTypeImplementation(@NotNull Class<?> implementation, @NotNull Class<?> superclass) {
            typeImplementations.add(Entry.of(Objects.requireNonNull(implementation), Objects
                    .requireNonNull(superclass)));
            return this;
        }

        public @NotNull Builder withSignatureBuilderPreference(@NotNull Class<?> type,
                @NotNull SignatureBuilder signatureBuilder) {
            signatureBuilderPreferences.add(Entry.of(Objects.requireNonNull(type), Objects
                    .requireNonNull(signatureBuilder)));
            return this;
        }

        public @NotNull MappingProcessorSource build() {
            SignatureMatcher.Source source = signatureMatcherSourceFunction.apply(customSignatures);
            TypeHinter hinter = this.typeHinter;
            TypeResolver resolver = typeResolverFunction.apply(typeImplementations);
            ScalarSource scalarSource = this.scalarSource;

            return new MappingProcessorSource() {
                @Override
                public @NotNull <T> ConfigProcessor<T> processorFor(@NotNull Token<T> token) {
                    return new MappingConfigProcessor<>(token, source, hinter, resolver, scalarSource);
                }
            };
        }
    }
}
