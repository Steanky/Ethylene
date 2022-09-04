package com.github.steanky.ethylene.core.mapper;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.core.mapper.signature.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Objects;

public class BasicCustomTypeMatcher implements TypeSignatureMatcher.Source {
    private final SignatureBuilder customSignatureBuilder;
    private final TypeHinter typeHinter;
    private final boolean matchNames;
    private final boolean matchTypeHints;

    public BasicCustomTypeMatcher(@NotNull SignatureBuilder customSignatureBuilder,
            @NotNull TypeHinter typeHinter, boolean matchNames, boolean matchTypeHints) {
        this.customSignatureBuilder = Objects.requireNonNull(customSignatureBuilder);
        this.typeHinter = Objects.requireNonNull(typeHinter);
        this.matchNames = matchNames;
        this.matchTypeHints = matchTypeHints;
    }

    @Override
    public @Nullable TypeSignatureMatcher matcherFor(@NotNull Type type, @NotNull ConfigElement element) {
        Signature[] signatures = customSignatureBuilder.buildSignatures(type);
        if (signatures.length == 0) {
            return null;
        }

        return new BasicTypeSignatureMatcher(signatures, typeHinter, matchNames, matchTypeHints);
    }
}
