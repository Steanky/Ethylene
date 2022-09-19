package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import com.github.steanky.ethylene.mapper.type.Token;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface SignatureMatcher {
    @NotNull MatchingSignature signature(@NotNull Token<?> desiredType, ConfigElement providedElement,
            Object providedObject);

    @FunctionalInterface
    interface Source {
        SignatureMatcher matcherFor(@NotNull Token<?> type);
    }
}
