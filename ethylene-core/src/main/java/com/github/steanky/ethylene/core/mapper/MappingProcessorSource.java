package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.core.mapper.signature.BasicSignatureBuilderSelector;
import com.github.steanky.ethylene.core.mapper.signature.Signature;
import com.github.steanky.ethylene.core.mapper.signature.SignatureBuilder;
import com.github.steanky.ethylene.core.mapper.signature.SignatureMatcher;
import com.github.steanky.ethylene.core.mapper.signature.field.FieldSignatureBuilder;
import com.github.steanky.ethylene.core.processor.ConfigProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface MappingProcessorSource {
    <TData> @NotNull ConfigProcessor<TData> processorFor(@NotNull Token<TData> token);

    static Builder builder() {
        return new Builder();
    }

    class Builder {
        private SignatureMatcher.Source signatureMatcherSource;
        private TypeHinter typeHinter = new BasicTypeHinter();
        private SignatureBuilder.Selector signatureBuilderSelector;
        private SignatureBuilder defaultBuilder;
        private TypeResolver typeResolver;

        private Set<Signature> customSignatures;
        private Set<Entry<Class<?>, Class<?>>> typeImplementations;

        Builder() {}

        public @NotNull Builder withSignatureMatcherSource(@NotNull SignatureMatcher.Source source) {
            this.signatureMatcherSource = Objects.requireNonNull(source);
            return this;
        }

        public @NotNull Builder withTypeHinter(@NotNull TypeHinter typeHinter) {
            this.typeHinter = Objects.requireNonNull(typeHinter);
            return this;
        }

        public @NotNull Builder withSignatureBuilderSelector(@NotNull SignatureBuilder.Selector selector) {
            this.signatureBuilderSelector = Objects.requireNonNull(selector);
            return this;
        }

        public @NotNull Builder withDefaultBuilder(@NotNull SignatureBuilder defaultBuilder) {
            this.defaultBuilder = Objects.requireNonNull(defaultBuilder);
            return this;
        }

        public @NotNull Builder withTypeResolver(@NotNull TypeResolver typeResolver) {
            this.typeResolver = Objects.requireNonNull(typeResolver);
            return this;
        }

        public @NotNull Builder withCustomSignature(@NotNull Signature signature) {
            Objects.requireNonNull(signature);
            if (customSignatures == null) {
                customSignatures = new HashSet<>(4);
            }

            customSignatures.add(signature);
            return this;
        }

        public @NotNull Builder withTypeImplementation(@NotNull Class<?> superclass, @NotNull Class<?> implementation) {
            Objects.requireNonNull(superclass);
            Objects.requireNonNull(implementation);
            if (typeImplementations == null) {
                typeImplementations = new HashSet<>(4);
            }

            typeImplementations.add(Entry.of(superclass, implementation));
            return this;
        }

        public @NotNull Builder withStandardTypeImplementations() {
            withTypeImplementation(Collection.class, ArrayList.class);
            withTypeImplementation(Set.class, HashSet.class);
            withTypeImplementation(Map.class, HashMap.class);
            return this;
        }

        @SuppressWarnings("rawtypes")
        public @NotNull Builder withStandardSignatures() {
            withCustomSignature(Signature.builder(new Token<Map.Entry>() {}, (entry, objects) ->
                            Map.entry(objects[0], objects[1]), (entry) ->
                            List.of(Signature.typed("key", Object.class, entry.getKey()), Signature.typed("value",
                    Object.class, entry.getValue())), Entry.of("key", new Token<>() {}), Entry.of("value", new Token<>() {}))
                    .matchingTypeHints()
                    .matchingNames()
                    .build());
            return this;
        }

        private SignatureMatcher.Source makeSource() {
            return Objects.requireNonNullElseGet(signatureMatcherSource,
                    () -> signatureMatcherSource = new BasicSignatureMatcherSource(typeHinter, makeSelector()));

        }

        private SignatureBuilder.Selector makeSelector() {
            return Objects.requireNonNullElseGet(signatureBuilderSelector,
                    () -> signatureBuilderSelector = new BasicSignatureBuilderSelector(makeDefaultBuilder()));

        }

        private SignatureBuilder makeDefaultBuilder() {
            return Objects.requireNonNullElseGet(defaultBuilder, () -> defaultBuilder = FieldSignatureBuilder.INSTANCE);

        }

        private TypeResolver makeTypeResolver() {
            return Objects.requireNonNullElseGet(typeResolver, () -> typeResolver = new BasicTypeResolver(typeHinter));

        }

        public @NotNull MappingProcessorSource build() {
            SignatureMatcher.Source source = makeSource();
            if (customSignatures != null) {
                for (Signature customSignature : customSignatures) {
                    source.registerCustomSignature(customSignature);
                }
            }

            TypeHinter hinter = typeHinter;
            TypeResolver resolver = makeTypeResolver();
            if (typeImplementations != null) {
                for (Entry<Class<?>, Class<?>> entry : typeImplementations) {
                    resolver.registerTypeImplementation(entry.getFirst(), entry.getSecond());
                }
            }

            return new MappingProcessorSource() {
                @Override
                public @NotNull <T> ConfigProcessor<T> processorFor(@NotNull Token<T> token) {
                    return new MappingConfigProcessor<>(token, source, hinter, resolver);
                }
            };
        }
    }
}
