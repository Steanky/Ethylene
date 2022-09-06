package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.mapper.signature.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Objects;

public class BasicCustomTypeMatcher implements SignatureMatcher.Source {
    private final SignatureBuilder customSignatureBuilder;
    private final TypeHinter typeHinter;

    public BasicCustomTypeMatcher(@NotNull SignatureBuilder customSignatureBuilder,
            @NotNull TypeHinter typeHinter) {
        this.customSignatureBuilder = Objects.requireNonNull(customSignatureBuilder);
        this.typeHinter = Objects.requireNonNull(typeHinter);
    }

    @Override
    public @Nullable SignatureMatcher matcherFor(@NotNull Type type, @Nullable ConfigElement element) {
        Signature[] signatures = customSignatureBuilder.buildSignatures(type);
        if (signatures.length == 0) {
            return null;
        }

        return new BasicSignatureMatcher(signatures, typeHinter);
    }
}
