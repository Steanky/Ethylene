package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public record SignatureElement(@NotNull Type type, @NotNull Object identifier) {}
