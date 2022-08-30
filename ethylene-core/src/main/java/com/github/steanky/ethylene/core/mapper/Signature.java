package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

public record Signature(int index, @NotNull SignatureElement[] elements, boolean indexed) {}
