package com.github.steanky.ethylene.mapper.signature;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.steanky.ethylene.core.collection.Entry;
import com.github.steanky.ethylene.mapper.annotation.Builder;
import com.github.steanky.ethylene.mapper.signature.constructor.ConstructorSignatureBuilder;
import com.github.steanky.ethylene.mapper.signature.field.FieldSignatureBuilder;
import com.github.steanky.ethylene.mapper.signature.record.RecordSignatureBuilder;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

public class BasicSignatureBuilderSelector implements SignatureBuilder.Selector {
    private final SignatureBuilder defaultBuilder;
    private final Cache<Class<?>, SignatureBuilder> builderTypeCache;

    public BasicSignatureBuilderSelector(@NotNull SignatureBuilder defaultBuilder,
        @NotNull Collection<Entry<Class<?>, SignatureBuilder>> signaturePreferences) {
        this.defaultBuilder = Objects.requireNonNull(defaultBuilder);
        this.builderTypeCache = Caffeine.newBuilder().initialCapacity(signaturePreferences.size()).weakKeys().build();

        registerSignaturePreferences(signaturePreferences);
    }

    private void registerSignaturePreferences(Collection<Entry<Class<?>, SignatureBuilder>> signaturePreferences) {
        for (Entry<Class<?>, SignatureBuilder> preference : signaturePreferences) {
            Class<?> type = preference.getKey();
            SignatureBuilder signatureBuilder = preference.getValue();

            Objects.requireNonNull(type);
            Objects.requireNonNull(signatureBuilder);
            builderTypeCache.put(type, signatureBuilder);
        }
    }

    @Override
    public @NotNull SignatureBuilder select(@NotNull Token<?> type) {
        Class<?> rawType = type.rawType();
        Builder builderAnnotation = rawType.getAnnotation(Builder.class);
        if (builderAnnotation == null) {
            SignatureBuilder signatureBuilder = builderTypeCache.getIfPresent(rawType);
            if (signatureBuilder == null && rawType.isRecord()) {
                return RecordSignatureBuilder.INSTANCE;
            }

            return signatureBuilder == null ? defaultBuilder : signatureBuilder;
        }

        return switch (builderAnnotation.value()) {
            case FIELD -> FieldSignatureBuilder.INSTANCE;
            case CONSTRUCTOR -> ConstructorSignatureBuilder.INSTANCE;
            case RECORD -> RecordSignatureBuilder.INSTANCE;
        };
    }
}
