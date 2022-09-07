package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.mapper.annotation.Builder;
import com.github.steanky.ethylene.core.mapper.signature.constructor.ConstructorSignatureBuilder;
import com.github.steanky.ethylene.core.mapper.signature.field.FieldSignatureBuilder;
import com.github.steanky.ethylene.core.mapper.signature.record.RecordSignatureBuilder;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Objects;

public class BasicSignatureBuilderSelector implements SignatureBuilder.Selector {
    private final SignatureBuilder defaultBuilder;

    public BasicSignatureBuilderSelector(@NotNull SignatureBuilder defaultBuilder) {
        this.defaultBuilder = Objects.requireNonNull(defaultBuilder);
    }

    @Override
    public @NotNull SignatureBuilder select(@NotNull Type type) {
        Builder builderAnnotation = TypeUtils.getRawType(type, null).getAnnotation(Builder.class);
        if (builderAnnotation == null) {
            return defaultBuilder;
        }

        return switch (builderAnnotation.value()) {
            case FIELD -> FieldSignatureBuilder.INSTANCE;
            case CONSTRUCTOR -> ConstructorSignatureBuilder.INSTANCE;
            case RECORD -> RecordSignatureBuilder.INSTANCE;
        };
    }
}
