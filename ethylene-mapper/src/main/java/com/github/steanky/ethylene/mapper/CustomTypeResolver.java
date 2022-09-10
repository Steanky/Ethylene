package com.github.steanky.ethylene.mapper;

import com.github.steanky.ethylene.mapper.signature.SignatureMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

@FunctionalInterface
public interface CustomTypeResolver {
    @Nullable SignatureMatcher matcherFor(@NotNull Type type);
}
