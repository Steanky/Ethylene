package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

@FunctionalInterface
public interface SignatureMatcher {
    @NotNull MatchingSignature signature(@NotNull Type desiredType, ConfigElement providedElement,
            Object providedObject);

    @FunctionalInterface
    interface Source {
        SignatureMatcher matcherFor(@NotNull Type type, @Nullable ConfigElement element);
    }
}
