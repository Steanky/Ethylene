package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

public interface SignatureMatcher {
    @NotNull MatchingSignature signature(@NotNull Type desiredType, ConfigElement providedElement,
            Object providedObject);

    interface Source {
        SignatureMatcher matcherFor(@NotNull Type type, @Nullable ConfigElement element);

        void registerCustomSignature(@NotNull Signature signature);
    }
}
