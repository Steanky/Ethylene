package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

public record Signature(int index, boolean indexed, @NotNull SignatureElement... elements) {}
