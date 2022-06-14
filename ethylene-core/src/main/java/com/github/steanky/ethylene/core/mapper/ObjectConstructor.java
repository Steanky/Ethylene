package com.github.steanky.ethylene.core.mapper;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record ObjectConstructor(SignatureElement @NotNull [] spec, @NotNull Function<Object[], Object> builder) { }
