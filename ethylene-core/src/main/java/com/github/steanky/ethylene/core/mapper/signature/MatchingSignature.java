package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

public record MatchingSignature(@NotNull Signature signature, Iterable<ConfigElement> elements,
        Iterable<Object> objects, int size) {}