package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import com.github.steanky.ethylene.mapper.signature.*;
import com.github.steanky.ethylene.mapper.signature.field.FieldSignatureBuilder;
import com.github.steanky.ethylene.mapper.type.Token;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public interface MappingProcessorSource {
    static @NotNull Builder builder() {
        return new Builder();
    }

    <TData> @NotNull ConfigProcessor<TData> processorFor(@NotNull Token<TData> token);

    class Builder {
        private static final Signature MAP_ENTRY_SIGNATURE =
            Signature.builder(Token.ofClass(Map.Entry.class), (entry, objects) -> Entry.of(objects[0], objects[1]),
                (entry) -> List.of(Signature.type("key", Token.OBJECT, entry.getKey()),
                    Signature.type("value", Token.OBJECT, entry.getValue())), Entry.of("key", Token.OBJECT),
                Entry.of("value", Token.OBJECT)).matchingTypeHints().matchingNames().build();

        private final Collection<Signature> customSignatures = new HashSet<>();
        private final Collection<Map.Entry<Class<?>, Class<?>>> typeImplementations = new HashSet<>();
        private final Collection<Map.Entry<Class<?>, SignatureBuilder>> signatureBuilderPreferences = new HashSet<>();
        private final Collection<Token<?>> scalarTypes = new HashSet<>();
        private final Collection<ScalarSignature<?>> scalarSignatures = new HashSet<>();

        private ScalarSource scalarSource;
        private TypeHinter typeHinter;
        private Function<? super Collection<Token<?>>, ? extends TypeHinter> typeHinterFunction = BasicTypeHinter::new;

        private SignatureBuilder defaultBuilder = FieldSignatureBuilder.INSTANCE;
        private Function<? super Collection<ScalarSignature<?>>, ? extends ScalarSource> scalarSourceFunction =
            signatures -> new BasicScalarSource(buildTypeHinter(), signatures);
        private Function<? super Collection<Map.Entry<Class<?>, SignatureBuilder>>, ? extends SignatureBuilder.Selector>
            signatureBuilderSelectorFunction =
            signaturePreferences -> new BasicSignatureBuilderSelector(defaultBuilder, signaturePreferences);
        private Function<? super Collection<Signature>, ? extends SignatureMatcher.Source>
            signatureMatcherSourceFunction =
            customSignatures -> new BasicSignatureMatcherSource(buildTypeHinter(), getSignatureBuilderSelector(),
                customSignatures);
        private Function<? super Collection<Map.Entry<Class<?>, Class<?>>>, ? extends TypeResolver>
            typeResolverFunction = typeImplementations -> new BasicTypeResolver(buildTypeHinter(), typeImplementations);

        Builder() {
        }

        private ScalarSource buildScalarSource() {
            return Objects.requireNonNullElseGet(scalarSource,
                () -> scalarSource = scalarSourceFunction.apply(scalarSignatures));
        }

        private TypeHinter buildTypeHinter() {
            return Objects.requireNonNullElseGet(typeHinter, () -> typeHinter = typeHinterFunction.apply(scalarTypes));
        }

        private SignatureBuilder.Selector getSignatureBuilderSelector() {
            return signatureBuilderSelectorFunction.apply(signatureBuilderPreferences);
        }

        public @NotNull Builder withTypeHinterFunction(
            @NotNull Function<? super Collection<Token<?>>, ? extends TypeHinter> typeHinterFunction) {
            this.typeHinterFunction = Objects.requireNonNull(typeHinterFunction);
            return this;
        }

        public @NotNull Builder withStandardSignatures() {
            customSignatures.add(MAP_ENTRY_SIGNATURE);
            return this;
        }

        public @NotNull Builder withStandardTypeImplementations() {
            typeImplementations.add(Map.entry(ArrayList.class, Collection.class));
            typeImplementations.add(Map.entry(HashMap.class, Map.class));
            typeImplementations.add(Map.entry(HashSet.class, Set.class));
            return this;
        }

        public @NotNull Builder withSignatureBuilderSelectorFunction(
            @NotNull Function<? super Collection<Map.Entry<Class<?>, SignatureBuilder>>, ?
                extends SignatureBuilder.Selector> function) {
            this.signatureBuilderSelectorFunction = Objects.requireNonNull(function);
            return this;
        }

        public @NotNull Builder withTypeResolverFunction(
            @NotNull Function<? super Collection<Map.Entry<Class<?>, Class<?>>>, ? extends TypeResolver> function) {
            this.typeResolverFunction = Objects.requireNonNull(function);
            return this;
        }

        public @NotNull Builder withScalarSourceFunction(
            @NotNull Function<? super Collection<ScalarSignature<?>>, ? extends ScalarSource> scalarSourceFunction) {
            this.scalarSourceFunction = Objects.requireNonNull(scalarSourceFunction);
            return this;
        }

        public @NotNull Builder withDefaultBuilder(@NotNull SignatureBuilder defaultBuilder) {
            this.defaultBuilder = Objects.requireNonNull(defaultBuilder);
            return this;
        }

        public @NotNull Builder withSignatureMatcherSourceFunction(
            @NotNull Function<? super Collection<Signature>, ? extends SignatureMatcher.Source> function) {
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

        public @NotNull Builder withScalarSignature(@NotNull ScalarSignature<?> signature) {
            scalarSignatures.add(signature);
            return this;
        }

        public @NotNull Builder withTypeImplementation(@NotNull Class<?> implementation, @NotNull Class<?> superclass) {
            typeImplementations.add(Map.entry(implementation, superclass));
            return this;
        }

        public @NotNull Builder withSignatureBuilderPreference(@NotNull Class<?> type,
            @NotNull SignatureBuilder signatureBuilder) {
            signatureBuilderPreferences.add(Map.entry(type, signatureBuilder));
            return this;
        }

        public @NotNull MappingProcessorSource build() {
            SignatureMatcher.Source source = signatureMatcherSourceFunction.apply(customSignatures);
            TypeHinter hinter = buildTypeHinter();
            TypeResolver resolver = typeResolverFunction.apply(typeImplementations);
            ScalarSource scalarSource = buildScalarSource();

            return new MappingProcessorSource() {
                @Override
                public @NotNull <T> ConfigProcessor<T> processorFor(@NotNull Token<T> token) {
                    return new MappingConfigProcessor<>(token, source, hinter, resolver, scalarSource);
                }
            };
        }
    }
}
