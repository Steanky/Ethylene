package com.github.steanky.ethylene.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public record MatchingSignature(@NotNull Signature signature, Collection<ConfigElement> elements,
        Collection<Signature.TypedObject> objects, int size) {}