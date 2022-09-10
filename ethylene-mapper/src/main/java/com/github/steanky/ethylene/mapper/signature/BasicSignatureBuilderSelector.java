package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.mapper.annotation.Builder;
import com.github.steanky.ethylene.mapper.signature.constructor.ConstructorSignatureBuilder;
import com.github.steanky.ethylene.mapper.signature.field.FieldSignatureBuilder;
import com.github.steanky.ethylene.mapper.signature.record.RecordSignatureBuilder;
import com.github.steanky.ethylene.mapper.util.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class BasicSignatureBuilderSelector implements SignatureBuilder.Selector {
    private final SignatureBuilder defaultBuilder;
    private final Map<Class<?>, SignatureBuilder> builderTypeMap;

    public BasicSignatureBuilderSelector(@NotNull SignatureBuilder defaultBuilder) {
        this.defaultBuilder = Objects.requireNonNull(defaultBuilder);
        this.builderTypeMap = new WeakHashMap<>();
    }

    @Override
    public @NotNull SignatureBuilder select(@NotNull Type type) {
        Class<?> rawType = ReflectionUtils.rawType(type);
        Builder builderAnnotation = rawType.getAnnotation(Builder.class);
        if (builderAnnotation == null) {
            SignatureBuilder signatureBuilder = builderTypeMap.get(rawType);
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

    @Override
    public void registerSignaturePreference(@NotNull Class<?> type, @NotNull SignatureBuilder signatureBuilder) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(signatureBuilder);
        builderTypeMap.put(type, signatureBuilder);
    }
}
