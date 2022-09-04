package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

@FunctionalInterface
public interface TypeSignatureMatcher {
    @NotNull MatchingSignature signature(@NotNull ConfigElement providedElement, @NotNull Type desiredType);

    @FunctionalInterface
    interface Source {
        TypeSignatureMatcher matcherFor(@NotNull Type type, @NotNull ConfigElement element);
    }
}
