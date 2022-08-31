package com.github.steanky.ethylene.core.mapper.signature;

import com.github.steanky.ethylene.core.ConfigElement;
import org.jetbrains.annotations.NotNull;

public record OrderedSignature(@NotNull Signature signature, @NotNull Iterable<ConfigElement> elementIterable,
        int size) {}
