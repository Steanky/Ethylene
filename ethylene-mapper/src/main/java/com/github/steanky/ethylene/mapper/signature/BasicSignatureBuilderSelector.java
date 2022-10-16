package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.mapper.annotation.Builder;
import com.github.steanky.ethylene.mapper.signature.constructor.ConstructorSignatureBuilder;
import com.github.steanky.ethylene.mapper.signature.field.FieldSignatureBuilder;
import com.github.steanky.ethylene.mapper.signature.record.RecordSignatureBuilder;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Basic implementation of {@link SignatureBuilder.Selector}. Uses the {@link Builder} annotation to determine which
 * {@link SignatureBuilder} to use for a given type. If this annotation is not present, the default builder will be
 * used, <i>unless</i> the type is a record, in which case {@link RecordSignatureBuilder} will be used.
 */
public class BasicSignatureBuilderSelector implements SignatureBuilder.Selector {
    private final SignatureBuilder defaultBuilder;
    private final Map<Class<?>, SignatureBuilder> builderTypeCache;

    /**
     * Creates a new instance of this class.
     *
     * @param defaultBuilder       the default builder, to be used when an unrecognized type is passed
     * @param signaturePreferences a collection of class-signature builder pairs corresponding to recognized types and
     *                             their preferred builders
     */
    public BasicSignatureBuilderSelector(@NotNull SignatureBuilder defaultBuilder,
        @NotNull Collection<? extends Map.Entry<Class<?>, ? extends SignatureBuilder>> signaturePreferences) {
        this.defaultBuilder = Objects.requireNonNull(defaultBuilder);
        this.builderTypeCache = new WeakHashMap<>(signaturePreferences.size(), 1F);

        registerSignaturePreferences(signaturePreferences);
    }

    private void registerSignaturePreferences(
        Collection<? extends Map.Entry<Class<?>, ? extends SignatureBuilder>> signaturePreferences) {
        for (Map.Entry<Class<?>, ? extends SignatureBuilder> preference : signaturePreferences) {
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
            SignatureBuilder signatureBuilder = builderTypeCache.get(rawType);

            //use RecordSignatureBuilder for records by default, regardless of defaultBuilder
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
