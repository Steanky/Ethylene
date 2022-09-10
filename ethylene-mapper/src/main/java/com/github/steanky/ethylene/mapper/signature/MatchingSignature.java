package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

public record MatchingSignature(@NotNull Signature signature, Iterable<ConfigElement> elements,
        Iterable<Signature.TypedObject> objects, int size) {}